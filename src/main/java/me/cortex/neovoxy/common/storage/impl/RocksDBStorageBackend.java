package me.cortex.neovoxy.common.storage.impl;

import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.common.storage.StorageBackend;
import org.rocksdb.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * RocksDB-based storage backend.
 * 
 * <p>Uses RocksDB for high-performance persistent storage.
 * Well-suited for SSDs with good write amplification characteristics.
 */
public class RocksDBStorageBackend extends StorageBackend {
    
    static {
        RocksDB.loadLibrary();
    }
    
    private final RocksDB db;
    private final WriteOptions writeOptions;
    private final ReadOptions readOptions;
    
    public RocksDBStorageBackend(Path dbPath) throws RocksDBException, IOException {
        super(dbPath);
        
        // Ensure directory exists
        Files.createDirectories(dbPath);
        
        // Configure RocksDB options
        try (Options options = new Options()) {
            options.setCreateIfMissing(true);
            options.setCompressionType(CompressionType.LZ4_COMPRESSION);
            options.setBottommostCompressionType(CompressionType.ZSTD_COMPRESSION);
            options.setMaxOpenFiles(256);
            options.setWriteBufferSize(64 * 1024 * 1024); // 64 MB
            options.setMaxWriteBufferNumber(4);
            options.setTargetFileSizeBase(64 * 1024 * 1024);
            
            // Optimize for bulk loads
            options.setAllowConcurrentMemtableWrite(true);
            options.setEnableWriteThreadAdaptiveYield(true);
            
            this.db = RocksDB.open(options, dbPath.toString());
        }
        
        this.writeOptions = new WriteOptions();
        this.writeOptions.setSync(false);
        this.writeOptions.setDisableWAL(false);
        
        this.readOptions = new ReadOptions();
        this.readOptions.setVerifyChecksums(false);
        
        Logger.info("RocksDB storage opened at: {}", dbPath);
    }
    
    @Override
    public byte[] get(long key) {
        try {
            return db.get(readOptions, keyToBytes(key));
        } catch (RocksDBException e) {
            Logger.error("RocksDB get failed for key: {}", key, e);
            return null;
        }
    }
    
    @Override
    public int get(long key, ByteBuffer buffer) {
        byte[] data = get(key);
        if (data == null) {
            return -1;
        }
        buffer.put(data);
        return data.length;
    }
    
    @Override
    public void put(long key, byte[] data) {
        try {
            db.put(writeOptions, keyToBytes(key), data);
        } catch (RocksDBException e) {
            Logger.error("RocksDB put failed for key: {}", key, e);
        }
    }
    
    @Override
    public void put(long key, ByteBuffer data) {
        byte[] arr = new byte[data.remaining()];
        data.get(arr);
        put(key, arr);
    }
    
    @Override
    public void delete(long key) {
        try {
            db.delete(writeOptions, keyToBytes(key));
        } catch (RocksDBException e) {
            Logger.error("RocksDB delete failed for key: {}", key, e);
        }
    }
    
    @Override
    public boolean exists(long key) {
        try {
            return db.get(readOptions, keyToBytes(key)) != null;
        } catch (RocksDBException e) {
            return false;
        }
    }
    
    @Override
    public void flush() {
        try {
            db.flush(new FlushOptions().setWaitForFlush(true));
        } catch (RocksDBException e) {
            Logger.error("RocksDB flush failed", e);
        }
    }
    
    @Override
    public long getStorageSize() {
        try {
            return Long.parseLong(db.getProperty("rocksdb.total-sst-files-size"));
        } catch (RocksDBException e) {
            return -1;
        }
    }
    
    private byte[] keyToBytes(long key) {
        byte[] bytes = new byte[8];
        bytes[0] = (byte) (key >>> 56);
        bytes[1] = (byte) (key >>> 48);
        bytes[2] = (byte) (key >>> 40);
        bytes[3] = (byte) (key >>> 32);
        bytes[4] = (byte) (key >>> 24);
        bytes[5] = (byte) (key >>> 16);
        bytes[6] = (byte) (key >>> 8);
        bytes[7] = (byte) key;
        return bytes;
    }
    
    @Override
    public void close() {
        Logger.info("Closing RocksDB storage...");
        
        writeOptions.close();
        readOptions.close();
        db.close();
        
        Logger.info("RocksDB storage closed");
    }
}
