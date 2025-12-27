package me.cortex.neovoxy.common.voxelization;

/**
 * Represents a voxelized chunk section ready for LOD mesh generation.
 * 
 * <p>Contains packed block data for a 16x16x16 section.
 */
public class VoxelizedSection {
    
    private final int chunkX;
    private final int sectionY;
    private final int chunkZ;
    
    // Packed data arrays (4096 entries for 16x16x16)
    private final int[] stateIds;
    private final int[] biomeIds;
    private final int[] lightLevels;
    
    private final int blockCount;
    
    public VoxelizedSection(int chunkX, int sectionY, int chunkZ,
                            int[] stateIds, int[] biomeIds, int[] lightLevels,
                            int blockCount) {
        this.chunkX = chunkX;
        this.sectionY = sectionY;
        this.chunkZ = chunkZ;
        this.stateIds = stateIds;
        this.biomeIds = biomeIds;
        this.lightLevels = lightLevels;
        this.blockCount = blockCount;
    }
    
    public int getChunkX() {
        return chunkX;
    }
    
    public int getSectionY() {
        return sectionY;
    }
    
    public int getChunkZ() {
        return chunkZ;
    }
    
    /**
     * Get block state ID at position.
     */
    public int getStateId(int x, int y, int z) {
        return stateIds[(y << 8) | (z << 4) | x];
    }
    
    /**
     * Get biome ID at position.
     */
    public int getBiomeId(int x, int y, int z) {
        return biomeIds[(y << 8) | (z << 4) | x];
    }
    
    /**
     * Get packed light level at position.
     */
    public int getLightLevel(int x, int y, int z) {
        return lightLevels[(y << 8) | (z << 4) | x];
    }
    
    /**
     * Check if position has a non-air block.
     */
    public boolean hasBlock(int x, int y, int z) {
        return stateIds[(y << 8) | (z << 4) | x] != 0;
    }
    
    /**
     * Get total non-air block count.
     */
    public int getBlockCount() {
        return blockCount;
    }
    
    /**
     * Check if section is empty.
     */
    public boolean isEmpty() {
        return blockCount == 0;
    }
    
    /**
     * Get section position packed into a long.
     */
    public long getPackedPosition() {
        return packPosition(chunkX, sectionY, chunkZ);
    }
    
    /**
     * Pack section coordinates into a long.
     */
    public static long packPosition(int chunkX, int sectionY, int chunkZ) {
        return ((long) chunkX & 0x3FFFFF) | 
               (((long) sectionY & 0xFF) << 22) |
               (((long) chunkZ & 0x3FFFFF) << 30);
    }
    
    /**
     * Unpack chunk X from packed position.
     */
    public static int unpackX(long packed) {
        int x = (int) (packed & 0x3FFFFF);
        return (x << 10) >> 10; // Sign extend
    }
    
    /**
     * Unpack section Y from packed position.
     */
    public static int unpackY(long packed) {
        int y = (int) ((packed >> 22) & 0xFF);
        return (byte) y; // Sign extend
    }
    
    /**
     * Unpack chunk Z from packed position.
     */
    public static int unpackZ(long packed) {
        int z = (int) ((packed >> 30) & 0x3FFFFF);
        return (z << 10) >> 10; // Sign extend
    }
}
