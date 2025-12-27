package me.cortex.neovoxy.common.world.storage;

import me.cortex.neovoxy.common.Logger;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.lmdb.*;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.*;
import static org.lwjgl.util.lmdb.LMDB.*;

/**
 * Storage backend using LMDB for persistent section storage.
 * LMDB is available via LWJGL and doesn't require separate native library
 * bundling.
 */
public class StorageBackend implements AutoCloseable {
    private static final long MAP_SIZE = 1024L * 1024L * 1024L * 10L; // 10 GB max

    private final long env;
    private final int dbi;
    private final AtomicBoolean isClosed = new AtomicBoolean(false);

    public StorageBackend(Path storagePath) throws IOException {
        Files.createDirectories(storagePath);

        try (MemoryStack stack = stackPush()) {
            var ppEnv = stack.callocPointer(1);

            int rc = mdb_env_create(ppEnv);
            if (rc != MDB_SUCCESS) {
                throw new RuntimeException("Failed to create LMDB environment: " + mdb_strerror(rc));
            }
            this.env = ppEnv.get(0);

            // Set map size
            rc = mdb_env_set_mapsize(env, MAP_SIZE);
            if (rc != MDB_SUCCESS) {
                mdb_env_close(env);
                throw new RuntimeException("Failed to set map size: " + mdb_strerror(rc));
            }

            // Open environment
            rc = mdb_env_open(env, storagePath.toString(), MDB_NOSYNC | MDB_WRITEMAP, 0664);
            if (rc != MDB_SUCCESS) {
                mdb_env_close(env);
                throw new RuntimeException("Failed to open LMDB environment: " + mdb_strerror(rc));
            }

            // Open database
            var ppTxn = stack.callocPointer(1);
            rc = mdb_txn_begin(env, NULL, 0, ppTxn);
            if (rc != MDB_SUCCESS) {
                mdb_env_close(env);
                throw new RuntimeException("Failed to begin transaction: " + mdb_strerror(rc));
            }
            long txn = ppTxn.get(0);

            var pDbi = stack.callocInt(1);
            rc = mdb_dbi_open(txn, (ByteBuffer) null, MDB_CREATE, pDbi);
            if (rc != MDB_SUCCESS) {
                mdb_txn_abort(txn);
                mdb_env_close(env);
                throw new RuntimeException("Failed to open database: " + mdb_strerror(rc));
            }
            this.dbi = pDbi.get(0);

            rc = mdb_txn_commit(txn);
            if (rc != MDB_SUCCESS) {
                mdb_env_close(env);
                throw new RuntimeException("Failed to commit transaction: " + mdb_strerror(rc));
            }
        }

        Logger.info("LMDB storage initialized at: {}", storagePath);
    }

    /**
     * Store voxel data for a section.
     */
    public void putSection(long sectionPos, byte[] data) {
        if (isClosed.get())
            return;

        try (MemoryStack stack = stackPush()) {
            var ppTxn = stack.callocPointer(1);
            int rc = mdb_txn_begin(env, NULL, 0, ppTxn);
            if (rc != MDB_SUCCESS) {
                Logger.error("Failed to begin write transaction: {}", mdb_strerror(rc));
                return;
            }
            long txn = ppTxn.get(0);

            MDBVal key = MDBVal.malloc(stack);
            MDBVal val = MDBVal.malloc(stack);

            ByteBuffer keyBuf = stack.malloc(8);
            keyBuf.putLong(0, sectionPos);
            key.mv_data(keyBuf);

            ByteBuffer valBuf = memAlloc(data.length);
            valBuf.put(data).flip();
            val.mv_data(valBuf);

            rc = mdb_put(txn, dbi, key, val, 0);
            memFree(valBuf);

            if (rc != MDB_SUCCESS) {
                mdb_txn_abort(txn);
                Logger.error("Failed to put section: {}", mdb_strerror(rc));
                return;
            }

            rc = mdb_txn_commit(txn);
            if (rc != MDB_SUCCESS) {
                Logger.error("Failed to commit write transaction: {}", mdb_strerror(rc));
            }
        }
    }

    /**
     * Retrieve voxel data for a section.
     */
    public byte[] getSection(long sectionPos) {
        if (isClosed.get())
            return null;

        try (MemoryStack stack = stackPush()) {
            var ppTxn = stack.callocPointer(1);
            int rc = mdb_txn_begin(env, NULL, MDB_RDONLY, ppTxn);
            if (rc != MDB_SUCCESS) {
                Logger.error("Failed to begin read transaction: {}", mdb_strerror(rc));
                return null;
            }
            long txn = ppTxn.get(0);

            MDBVal key = MDBVal.malloc(stack);
            MDBVal val = MDBVal.malloc(stack);

            ByteBuffer keyBuf = stack.malloc(8);
            keyBuf.putLong(0, sectionPos);
            key.mv_data(keyBuf);

            rc = mdb_get(txn, dbi, key, val);
            if (rc == MDB_NOTFOUND) {
                mdb_txn_abort(txn);
                return null;
            }
            if (rc != MDB_SUCCESS) {
                mdb_txn_abort(txn);
                Logger.error("Failed to get section: {}", mdb_strerror(rc));
                return null;
            }

            ByteBuffer dataBuf = val.mv_data();
            byte[] result = new byte[dataBuf.remaining()];
            dataBuf.get(result);

            mdb_txn_abort(txn); // Read-only txn, just abort
            return result;
        }
    }

    /**
     * Flush all pending writes to disk.
     */
    public void flush() {
        if (isClosed.get())
            return;
        mdb_env_sync(env, true);
    }

    @Override
    public void close() {
        if (isClosed.getAndSet(true))
            return;

        mdb_dbi_close(env, dbi);
        mdb_env_close(env);

        Logger.info("LMDB storage closed");
    }
}
