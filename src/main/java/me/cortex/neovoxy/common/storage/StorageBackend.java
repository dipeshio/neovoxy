package me.cortex.neovoxy.common.storage;

import me.cortex.neovoxy.common.Logger;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.file.Path;

/**
 * Abstract storage backend for LOD section data.
 * 
 * <p>Implementations use different databases:
 * <ul>
 *   <li>RocksDB - High performance, good for SSDs</li>
 *   <li>LMDB - Memory-mapped, excellent read performance</li>
 * </ul>
 */
public abstract class StorageBackend implements Closeable {
    
    protected final Path dbPath;
    
    protected StorageBackend(Path dbPath) {
        this.dbPath = dbPath;
    }
    
    /**
     * Get section data by packed position key.
     * 
     * @param key Packed section position
     * @return Section data or null if not found
     */
    public abstract byte[] get(long key);
    
    /**
     * Get section data into existing buffer.
     * 
     * @param key Packed section position
     * @param buffer Buffer to read into
     * @return Number of bytes read, or -1 if not found
     */
    public abstract int get(long key, ByteBuffer buffer);
    
    /**
     * Store section data.
     * 
     * @param key Packed section position
     * @param data Section data
     */
    public abstract void put(long key, byte[] data);
    
    /**
     * Store section data from buffer.
     * 
     * @param key Packed section position
     * @param data Section data buffer
     */
    public abstract void put(long key, ByteBuffer data);
    
    /**
     * Delete section data.
     * 
     * @param key Packed section position
     */
    public abstract void delete(long key);
    
    /**
     * Check if section exists.
     * 
     * @param key Packed section position
     * @return True if section exists
     */
    public abstract boolean exists(long key);
    
    /**
     * Flush any pending writes to disk.
     */
    public abstract void flush();
    
    /**
     * Get the database path.
     */
    public Path getPath() {
        return dbPath;
    }
    
    /**
     * Get estimated storage size in bytes.
     */
    public abstract long getStorageSize();
}
