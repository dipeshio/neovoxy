package me.cortex.neovoxy.common.world;

import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.common.world.other.Mapper;
import me.cortex.neovoxy.commonImpl.VoxyCommon;
import me.cortex.neovoxy.commonImpl.WorldIdentifier;

import java.io.Closeable;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core engine managing LOD data for a world.
 * 
 * <p>
 * Handles:
 * <ul>
 * <li>Section storage and retrieval</li>
 * <li>Block state to ID mapping</li>
 * <li>Dirty section tracking for updates</li>
 * <li>Reference counting for safe cleanup</li>
 * </ul>
 */
public class WorldEngine implements Closeable {

    private final WorldIdentifier worldId;
    private final Path storagePath;
    private final AtomicInteger refCount = new AtomicInteger(0);

    private Mapper mapper;
    private ISectionDirtyCallback dirtyCallback;

    private final me.cortex.neovoxy.common.world.storage.StorageBackend storage;
    private final me.cortex.neovoxy.common.world.storage.ActiveSectionTracker sectionTracker;

    private volatile boolean isClosed = false;

    public WorldEngine(WorldIdentifier worldId) {
        this.worldId = worldId;
        this.storagePath = VoxyCommon.getDataPath().resolve(worldId.toPathSafe());

        Logger.info("WorldEngine created for: {} at {}", worldId, storagePath);

        try {
            this.storage = new me.cortex.neovoxy.common.world.storage.StorageBackend(storagePath.resolve("db"));
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize storage backend", e);
        }

        this.sectionTracker = new me.cortex.neovoxy.common.world.storage.ActiveSectionTracker();

        // Load mapper data
        try {
            this.mapper = new Mapper();
            mapper.load(storagePath.resolve("mapper.bin"));
        } catch (Exception e) {
            Logger.error("Failed to load mapper data", e);
        }
    }

    /**
     * Set the block state / biome mapper.
     */
    public void setMapper(Mapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Get the mapper.
     */
    public Mapper getMapper() {
        return mapper;
    }

    /**
     * Get the storage backend.
     */
    public me.cortex.neovoxy.common.world.storage.StorageBackend getStorage() {
        return storage;
    }

    /**
     * Get the active section tracker.
     */
    public me.cortex.neovoxy.common.world.storage.ActiveSectionTracker getSectionTracker() {
        return sectionTracker;
    }

    /**
     * Acquire a reference to keep this engine alive.
     */
    public void acquireRef() {
        refCount.incrementAndGet();
    }

    /**
     * Release a reference. Engine closes when refcount hits 0.
     */
    public void releaseRef() {
        if (refCount.decrementAndGet() <= 0) {
            close();
        }
    }

    /**
     * Get the storage path for this world.
     */
    public Path getStoragePath() {
        return storagePath;
    }

    /**
     * Get the world identifier.
     */
    public WorldIdentifier getWorldId() {
        return worldId;
    }

    /**
     * Set callback for section dirty events.
     */
    public void setDirtyCallback(ISectionDirtyCallback callback) {
        this.dirtyCallback = callback;
    }

    /**
     * Notify that a section has been modified.
     */
    public void notifySectionDirty(long sectionPos) {
        sectionTracker.markDirty(sectionPos);
        if (dirtyCallback != null) {
            dirtyCallback.onSectionDirty(sectionPos);
        }
    }

    @Override
    public void close() {
        if (isClosed)
            return;
        isClosed = true;

        Logger.info("Closing WorldEngine for: {}", worldId);

        try {
            // Flush and close storage
            storage.flush();
            storage.close();

            // Save mapper data
            if (mapper != null) {
                mapper.save(storagePath.resolve("mapper.bin"));
            }
        } catch (Exception e) {
            Logger.error("Error during WorldEngine shutdown", e);
        }

        Logger.info("WorldEngine closed");
    }

    /**
     * Callback interface for section dirty events.
     */
    @FunctionalInterface
    public interface ISectionDirtyCallback {
        void onSectionDirty(long sectionPos);
    }
}
