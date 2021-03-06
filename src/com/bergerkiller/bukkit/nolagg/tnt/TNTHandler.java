package com.bergerkiller.bukkit.nolagg.tnt;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkPosition;
import net.minecraft.server.Packet60Explosion;
import net.minecraft.server.WorldServer;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.bergerkiller.bukkit.common.BlockSet;
import com.bergerkiller.bukkit.common.Task;
import com.bergerkiller.bukkit.common.utils.PacketUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;
import com.bergerkiller.bukkit.nolagg.NoLagg;

public class TNTHandler {

	private static Queue<Block> todo = new LinkedList<Block>();
	private static BlockSet added = new BlockSet();
	private static Task task;
	public static int interval;
	public static int rate;
	public static int explosionRate;
	private static long sentExplosions = 0;
	private static long intervalCounter = 0;
	public static boolean changeBlocks = true;

	public static int getBufferCount() {
		return todo.size();
	}
	
	public static void init() {
		//start the task
		if (interval > 0) {
			task = new Task(NoLagg.plugin) {
				public void run() {
					if (added == null) return;
					if (todo == null) return;
					if (denyExplosionsCounter > 0) {
						--denyExplosionsCounter;
					}
					sentExplosions = 0;
					if (intervalCounter == interval) {
						intervalCounter = 1;
						CustomExplosion.useQuickDamageMode = todo.size() > 500;
						if (todo.isEmpty()) return;
						for (int i = 0; i < rate; i++) {
							Block next = todo.poll();
							if (next == null) break;
							added.remove(next);
							int x = next.getX();
							int y = next.getY();
							int z = next.getZ();
							if (next.getWorld().isChunkLoaded(x >> 4, z >> 4)) {
								Chunk chunk = WorldUtil.getNative(next.getChunk());
								if (chunk.getTypeId(x & 15, y, z & 15) == Material.TNT.getId()) {
									chunk.world.setTypeId(x, y, z, 0);

									TNTPrimed tnt = next.getWorld().spawn(next.getLocation().add(0.5, 0.5, 0.5), TNTPrimed.class);
									int fuse = tnt.getFuseTicks();
									fuse = nextRandom(tnt.getWorld(), fuse >> 2) + fuse >> 3;
									tnt.setFuseTicks(fuse);
								}
							}
						}
					} else {
						intervalCounter++;
					}
				}
			}.start(1, 1);
		}
	}
		
	public static void deinit() {
		Task.stop(task);
		added = null;
		todo = null;
	}

	private static int nextRandom(World w, int n) {
		net.minecraft.server.World world = ((CraftWorld) w).getHandle();
		return world.random.nextInt(n);
	}

	private static int denyExplosionsCounter = 0; //tick countdown to deny explosions
	public static void clear() {
		todo.clear();
		added.clear();
		denyExplosionsCounter = 5;
	}
	
	public static boolean isScheduledForDetonation(Block block) {
		return added.contains(block);
	}

	/*
	 * Detonates TNT and creates explosions
	 * Returns false if it was not possible to do in any way
	 * (Including if the feature is disabled)
	 */
	public static boolean detonate(Block tntBlock) {
		if (added == null) return false;
		if (todo == null) return false;
		if (interval <= 0) return false;
		if (tntBlock != null) { // && tntBlock.getType() == Material.TNT) {
			if (added.add(tntBlock)) {
				todo.offer(tntBlock);
				return true;
			}
		}
		return false;
	}

	private static boolean allowdrops = true;

	public static boolean createExplosion(EntityExplodeEvent event) {
		return createExplosion(event.getLocation(), event.blockList(), event.getYield());
	}
	
	public static boolean createExplosion(Location at, List<Block> affectedBlocks, float yield) {
		if (interval > 0) {
			if (denyExplosionsCounter == 0) {
				try {
					WorldServer world = ((CraftWorld) at.getWorld()).getHandle();
					int id;
					int x, y, z;
					for (Block b : affectedBlocks) {
						id = b.getTypeId();
						x = b.getX();
						y = b.getY();
						z = b.getZ();
						if (id == Material.TNT.getId()) {
							detonate(b);
						} else {
							if (id != Material.FIRE.getId()) {
								net.minecraft.server.Block bb = net.minecraft.server.Block.byId[id];
								if (bb != null && allowdrops) {
									try {
										bb.dropNaturally(world, x, y, z, b.getData(), yield, 0);
									} catch (Throwable t) {
										NoLaggTNT.plugin.log(Level.SEVERE, "Failed to spawn block drops during explosions!");
										t.printStackTrace();
										allowdrops = false;
									}
								}
								b.setTypeId(0);
							}
						}
					}
					if (sentExplosions < explosionRate) {
						++sentExplosions;
						Packet60Explosion packet = new Packet60Explosion(at.getX(), at.getY(), at.getZ(), yield, new HashSet<ChunkPosition>());
						PacketUtil.broadcastPacketNearby(at, 64.0, packet);
					}
				} catch (Throwable t) {
					NoLaggTNT.plugin.log(Level.WARNING, "Explosion did not go as planned!");
					t.printStackTrace();
				}
			}
			return true;
		} else {
			return false;
		}
	}

}
