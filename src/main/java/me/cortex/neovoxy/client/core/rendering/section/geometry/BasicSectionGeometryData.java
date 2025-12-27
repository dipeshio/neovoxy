package me.cortex.neovoxy.client.core.rendering.section.geometry;

import me.cortex.neovoxy.client.core.gl.GlBuffer;
import me.cortex.neovoxy.common.Logger;

import java.util.BitSet;
import java.util.concurrent.locks.ReentrantLock;

import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.*;

/**
 * Basic implementation of section geometry data using a linear allocator.
 * 
 * <p>Each quad is 8 bytes (64 bits) packed as:
 * <ul>
 *   <li>Bits 0-2: Face (6 directions)</li>
 *   <li>Bits 3-6: Width (0-15, +1)</li>
 *   <li>Bits 7-10: Height (0-15, +1)</li>
 *   <li>Bits 11-25: Position (5 bits each for x, y, z)</li>
 *   <li>Bits 26-45: State ID (20 bits)</li>
 *   <li>Bits 46-54: Biome ID (9 bits)</li>
 *   <li>Bits 55-62: Light level (8 bits)</li>
 * </ul>
 */
public class BasicSectionGeometryData implements IGeometryData {
    
    // 8 bytes per quad
    private static final int BYTES_PER_QUAD = 8;
    
    // Allocation block size (in quads)
    private static final int BLOCK_SIZE = 1024;
    
    private final GlBuffer quadBuffer;
    private final long capacity;
    private final int maxBlocks;
    
    // Simple bitmap allocator
    private final BitSet allocatedBlocks;
    private final ReentrantLock allocLock = new ReentrantLock();
    
    private long currentUsage = 0;
    
    /**
     * Create geometry data storage.
     * 
     * @param maxSections Maximum number of sections to support
     * @param capacityBytes Total GPU memory capacity for quads
     */
    public BasicSectionGeometryData(int maxSections, long capacityBytes) {
        this.capacity = capacityBytes;
        this.maxBlocks = (int) (capacityBytes / (BLOCK_SIZE * BYTES_PER_QUAD));
        
        this.quadBuffer = new GlBuffer(capacityBytes, GL_DYNAMIC_STORAGE_BIT);
        this.allocatedBlocks = new BitSet(maxBlocks);
        
        Logger.info("BasicSectionGeometryData created: {} MB capacity, {} blocks", 
                    capacityBytes / (1024 * 1024), maxBlocks);
    }
    
    @Override
    public long capacity() {
        return capacity;
    }
    
    @Override
    public long usage() {
        return currentUsage;
    }
    
    @Override
    public int getQuadBufferId() {
        return quadBuffer.id();
    }
    
    @Override
    public long allocate(int quadCount) {
        if (quadCount <= 0) return -1;
        
        // Calculate blocks needed
        int blocksNeeded = (quadCount + BLOCK_SIZE - 1) / BLOCK_SIZE;
        
        allocLock.lock();
        try {
            // Find contiguous free blocks
            int startBlock = findFreeBlocks(blocksNeeded);
            if (startBlock < 0) {
                Logger.warn("Failed to allocate {} blocks for {} quads", blocksNeeded, quadCount);
                return -1;
            }
            
            // Mark blocks as allocated
            allocatedBlocks.set(startBlock, startBlock + blocksNeeded);
            currentUsage += (long) blocksNeeded * BLOCK_SIZE * BYTES_PER_QUAD;
            
            return (long) startBlock * BLOCK_SIZE * BYTES_PER_QUAD;
        } finally {
            allocLock.unlock();
        }
    }
    
    private int findFreeBlocks(int count) {
        int consecutive = 0;
        int startBlock = -1;
        
        for (int i = 0; i < maxBlocks; i++) {
            if (!allocatedBlocks.get(i)) {
                if (consecutive == 0) {
                    startBlock = i;
                }
                consecutive++;
                if (consecutive >= count) {
                    return startBlock;
                }
            } else {
                consecutive = 0;
                startBlock = -1;
            }
        }
        
        return -1;
    }
    
    @Override
    public void free(long offset, int quadCount) {
        if (offset < 0 || quadCount <= 0) return;
        
        int startBlock = (int) (offset / (BLOCK_SIZE * BYTES_PER_QUAD));
        int blocksToFree = (quadCount + BLOCK_SIZE - 1) / BLOCK_SIZE;
        
        allocLock.lock();
        try {
            allocatedBlocks.clear(startBlock, startBlock + blocksToFree);
            currentUsage -= (long) blocksToFree * BLOCK_SIZE * BYTES_PER_QUAD;
        } finally {
            allocLock.unlock();
        }
    }
    
    @Override
    public void upload(long offset, long[] data) {
        quadBuffer.upload(offset, data);
    }
    
    @Override
    public void close() {
        quadBuffer.close();
        Logger.info("BasicSectionGeometryData closed");
    }
}
