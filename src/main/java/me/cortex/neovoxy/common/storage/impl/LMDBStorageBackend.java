package me.cortex.neovoxy.common.storage.impl;

import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.common.storage.StorageBackend;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.lmdb.LMDB.*;

/**
 * LMDB-based storage backend.
 * 
 * <p>Uses Lightning Memory-Mapped Database for excellent read performance.
 * Memory-mapped architecture provides zero-copy reads.
 */
public class LMDBStorageBackend extends StorageBackend {
    
    // 1 GB default max size
    private static final long DEFAULT_MAP_SIZE = 1024L * 1024 * 1024;
    
    private long env;
    private int dbi;
    
    public LMDBStorageBackend(Path dbPath) throws IOException {
        this(dbPath, DEFAULT_MAP_SIZE);
    }
    
    public LMDBStorageBackend(Path dbPath, long maxSize) throws IOException {
        super(dbPath);
        
        // Ensure directory exists
        Files.createDirectories(dbPath);
        
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var ppEnv = stack.mallocPointer(1);
            
            int rc = mdb_env_create(ppEnv);
            if (rc != MDB_SUCCESS) {
                throw new RuntimeException("Failed to create LMDB environment: " + mdb_strerror(rc));
            }
            env = ppEnv.get(0);
            
            // Set map size
            rc = mdb_env_set_mapsize(env, maxSize);
            if (rc != MDB_SUCCESS) {
                mdb_env_close(env);
                throw new RuntimeException("Failed to set LMDB map size: " + mdb_strerror(rc));
            }
            
            // Open environment
            rc = mdb_env_open(env, dbPath.toString(), MDB_NOSYNC | MDB_WRITEMAP, 0664);
            if (rc != MDB_SUCCESS) {
                mdb_env_close(env);
                throw new RuntimeException("Failed to open LMDB environment: " + mdb_strerror(rc));
            }
            
            // Open database
            var ppTxn = stack.mallocPointer(1);
            rc = mdb_txn_begin(env, NULL, 0, ppTxn);
            if (rc != MDB_SUCCESS) {
                mdb_env_close(env);
                throw new RuntimeException("Failed to begin LMDB transaction: " + mdb_strerror(rc));
            }
            long txn = ppTxn.get(0);
            
            var pDbi = stack.mallocInt(1);
            rc = mdb_dbi_open(txn, (ByteBuffer) null, MDB_CREATE, pDbi);
            if (rc != MDB_SUCCESS) {
                mdb_txn_abort(txn);
                mdb_env_close(env);
                throw new RuntimeException("Failed to open LMDB database: " + mdb_strerror(rc));
            }
            dbi = pDbi.get(0);
            
            rc = mdb_txn_commit(txn);
            if (rc != MDB_SUCCESS) {
                mdb_env_close(env);
                throw new RuntimeException("Failed to commit LMDB transaction: " + mdb_strerror(rc));
            }
        }
        
        Logger.info("LMDB storage opened at: {} (max {} MB)", dbPath, maxSize / (1024 * 1024));
    }
    
    @Override
    public byte[] get(long key) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var ppTxn = stack.mallocPointer(1);
            int rc = mdb_txn_begin(env, NULL, MDB_RDONLY, ppTxn);
            if (rc != MDB_SUCCESS) return null;
            long txn = ppTxn.get(0);
            
            try {
                MDBVal keyVal = MDBVal.malloc(stack);
                keyVal.mv_data(keyToBuffer(stack, key));
                keyVal.mv_size(8);
                
                MDBVal dataVal = MDBVal.malloc(stack);
                
                rc = mdb_get(txn, dbi, keyVal, dataVal);
                if (rc == MDB_NOTFOUND) {
                    return null;
                }
                if (rc != MDB_SUCCESS) {
                    return null;
                }
                
                ByteBuffer data = dataVal.mv_data();
                byte[] result = new byte[(int) dataVal.mv_size()];
                data.get(result);
                return result;
            } finally {
                mdb_txn_abort(txn);
            }
        }
    }
    
    @Override
    public int get(long key, ByteBuffer buffer) {
        byte[] data = get(key);
        if (data == null) return -1;
        buffer.put(data);
        return data.length;
    }
    
    @Override
    public void put(long key, byte[] data) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var ppTxn = stack.mallocPointer(1);
            int rc = mdb_txn_begin(env, NULL, 0, ppTxn);
            if (rc != MDB_SUCCESS) {
                Logger.error("LMDB put begin failed: {}", mdb_strerror(rc));
                return;
            }
            long txn = ppTxn.get(0);
            
            MDBVal keyVal = MDBVal.malloc(stack);
            keyVal.mv_data(keyToBuffer(stack, key));
            keyVal.mv_size(8);
            
            MDBVal dataVal = MDBVal.malloc(stack);
            ByteBuffer dataBuf = stack.malloc(data.length);
            dataBuf.put(data).flip();
            dataVal.mv_data(dataBuf);
            dataVal.mv_size(data.length);
            
            rc = mdb_put(txn, dbi, keyVal, dataVal, 0);
            if (rc != MDB_SUCCESS) {
                mdb_txn_abort(txn);
                Logger.error("LMDB put failed: {}", mdb_strerror(rc));
                return;
            }
            
            rc = mdb_txn_commit(txn);
            if (rc != MDB_SUCCESS) {
                Logger.error("LMDB put commit failed: {}", mdb_strerror(rc));
            }
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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var ppTxn = stack.mallocPointer(1);
            int rc = mdb_txn_begin(env, NULL, 0, ppTxn);
            if (rc != MDB_SUCCESS) return;
            long txn = ppTxn.get(0);
            
            MDBVal keyVal = MDBVal.malloc(stack);
            keyVal.mv_data(keyToBuffer(stack, key));
            keyVal.mv_size(8);
            
            mdb_del(txn, dbi, keyVal, null);
            mdb_txn_commit(txn);
        }
    }
    
    @Override
    public boolean exists(long key) {
        return get(key) != null;
    }
    
    @Override
    public void flush() {
        mdb_env_sync(env, true);
    }
    
    @Override
    public long getStorageSize() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            MDBStat stat = MDBStat.malloc(stack);
            
            var ppTxn = stack.mallocPointer(1);
            if (mdb_txn_begin(env, NULL, MDB_RDONLY, ppTxn) != MDB_SUCCESS) {
                return -1;
            }
            long txn = ppTxn.get(0);
            
            try {
                if (mdb_stat(txn, dbi, stat) != MDB_SUCCESS) {
                    return -1;
                }
                return stat.ms_psize() * (stat.ms_branch_pages() + stat.ms_leaf_pages() + stat.ms_overflow_pages());
            } finally {
                mdb_txn_abort(txn);
            }
        }
    }
    
    private ByteBuffer keyToBuffer(MemoryStack stack, long key) {
        ByteBuffer buf = stack.malloc(8);
        buf.putLong(key).flip();
        return buf;
    }
    
    @Override
    public void close() {
        Logger.info("Closing LMDB storage...");
        
        mdb_dbi_close(env, dbi);
        mdb_env_close(env);
        
        Logger.info("LMDB storage closed");
    }
}
