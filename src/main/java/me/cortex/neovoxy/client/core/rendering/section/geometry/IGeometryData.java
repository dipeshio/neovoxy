package me.cortex.neovoxy.client.core.rendering.section.geometry;

/**
 * Interface for geometry data storage.
 * Manages GPU buffers containing quad data for LOD sections.
 */
public interface IGeometryData extends AutoCloseable {
    
    /**
     * Get the maximum capacity in bytes.
     */
    long capacity();
    
    /**
     * Get current usage in bytes.
     */
    long usage();
    
    /**
     * Get the quad buffer ID for binding.
     */
    int getQuadBufferId();
    
    /**
     * Allocate space for section geometry.
     * 
     * @param quadCount Number of quads to allocate
     * @return Offset in the buffer, or -1 if allocation failed
     */
    long allocate(int quadCount);
    
    /**
     * Free previously allocated section geometry.
     * 
     * @param offset Offset returned from allocate()
     * @param quadCount Original quad count
     */
    void free(long offset, int quadCount);
    
    /**
     * Upload quad data to allocated region.
     * 
     * @param offset Offset from allocate()
     * @param data Quad data (8 bytes per quad)
     */
    void upload(long offset, long[] data);
}
