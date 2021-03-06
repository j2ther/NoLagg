package com.bergerkiller.bukkit.nolagg.chunks;

import java.util.Collection;

import com.bergerkiller.bukkit.common.utils.PacketUtil;

import net.minecraft.server.Chunk;
import net.minecraft.server.Packet;
import net.minecraft.server.Packet51MapChunk;
import net.minecraft.server.TileEntity;

public class ChunkSendCommand {
    public ChunkSendCommand(final Packet51MapChunk mapPacket, final Chunk chunk) {
        this.mapPacket = mapPacket;
        this.chunk = chunk;
    }
    private final Packet51MapChunk mapPacket;
    public final Chunk chunk;
        
    public boolean isValid() {
    	return this.chunk != null && this.mapPacket != null;
    }
    
    public void send(final ChunkSendQueue queue) {
    	send(queue, this.mapPacket, this.chunk);
    }
    
    @SuppressWarnings("unchecked")
	public static void send(final ChunkSendQueue queue, final Packet51MapChunk mapPacket, final Chunk chunk) {
    	if (mapPacket == null) return;
    	PacketUtil.sendPacket(queue.ep, mapPacket, !NoLaggChunks.useBufferedLoading);
    	Packet p;
    	for (TileEntity tile : (Collection<TileEntity>) chunk.tileEntities.values()) {
    		if ((p = tile.d()) != null) {
    			PacketUtil.sendPacket(queue.ep, p, !NoLaggChunks.useBufferedLoading);
    		}
    	}
    }
    
}
