package com.bergerkiller.bukkit.nolagg.itembuffer;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.minecraft.server.Chunk;
import net.minecraft.server.ChunkCoordIntPair;
import net.minecraft.server.Entity;
import net.minecraft.server.EntityItem;

import com.bergerkiller.bukkit.common.utils.ItemUtil;
import com.bergerkiller.bukkit.common.utils.WorldUtil;

public class ChunkItems {
	
	public final Set<EntityItem> spawnedItems = new HashSet<EntityItem>();
	public final List<EntityItem> hiddenItems = new ArrayList<EntityItem>();
	public final Chunk chunk;
	
	public ChunkItems(org.bukkit.Chunk chunk) {
		this(WorldUtil.getNative(chunk));
	}
	@SuppressWarnings({"rawtypes", "unchecked"})
	public ChunkItems(final Chunk chunk) {
		this.chunk = chunk;
		for (List list : chunk.entitySlices) {
			for (Entity entity : (List<Entity>) list) {
				if (entity instanceof EntityItem) {
					if (entity.dead) continue;
					if (ItemUtil.isIgnored(entity.getBukkitEntity())) continue;
					if (this.spawnedItems.size() < NoLaggItemBuffer.maxItemsPerChunk) {
						this.spawnedItems.add((EntityItem) entity);
					} else {
						this.hiddenItems.add((EntityItem) entity);
						entity.dead = true;
					}
				}
			}
		}
	}
	public synchronized void deinit() {
		if (!this.hiddenItems.isEmpty()) {
			ChunkCoordIntPair coord;
			for (EntityItem item : this.hiddenItems) {
				item.dead = false;
				coord = ItemMap.getChunkCoords(item);
				this.chunk.world.getChunkAt(coord.x, coord.z);
				this.chunk.world.addEntity(item);
			}
			this.hiddenItems.clear();
		}
		this.spawnedItems.clear();
	}
	
	public synchronized void clear() {
		this.hiddenItems.clear();
	}
	
	public synchronized void spawnInChunk() {
		while (!hiddenItems.isEmpty() && spawnedItems.size() < NoLaggItemBuffer.maxItemsPerChunk) {
			spawnItem(hiddenItems.remove(0));
		}
	}
	
	public synchronized void spawnItem(EntityItem item) {
		ChunkCoordIntPair coord = ItemMap.getChunkCoords(item);
		this.chunk.world.getChunkAt(coord.x, coord.z);
		this.chunk.world.addEntity(item);
	}
	
	public synchronized void update() {
		if (this.hiddenItems.isEmpty()) return;
		if (!this.spawnedItems.isEmpty()) {
			Iterator<EntityItem> iter = this.spawnedItems.iterator();
			EntityItem e;
			ChunkCoordIntPair pair;
			while (iter.hasNext()) {
				e = iter.next();
				if (e.dead) {
					iter.remove();
				} else {
					pair = ItemMap.getChunkCoords(e);
					if (pair.x != this.chunk.x || pair.z != this.chunk.z) {
						//respawn in correct chunk
						iter.remove();
						ItemMap.addItem(pair, e);
					}
				}
			}
		}
		this.spawnInChunk();
	}

	public synchronized boolean handleSpawn(EntityItem item) {
		boolean allowed = false;
		if (this.spawnedItems.size() < NoLaggItemBuffer.maxItemsPerChunk) {
			allowed = true;
		} else {
			Iterator<EntityItem> iter = this.spawnedItems.iterator();
			EntityItem e;
			ChunkCoordIntPair pair;
			while (iter.hasNext()) {
				e = iter.next();
				if (e.dead) {
					iter.remove();
					allowed = true;
					break;
				} else {
					pair = ItemMap.getChunkCoords(e);
					if (pair.x != this.chunk.x || pair.z != this.chunk.z) {
						//respawn in correct chunk
						iter.remove();
						ItemMap.addItem(pair, e);
						allowed = true;
						break;
					}
				}
			}
		}
		if (allowed) {
			this.spawnedItems.add(item);
		} else {
			this.hiddenItems.add(item);
		}
		return allowed;
	}

}
