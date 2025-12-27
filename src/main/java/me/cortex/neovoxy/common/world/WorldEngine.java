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
 * <p>Handles:
 * <ul>
 *   <li>Section storage and retrieval</li>
 *   <li>Block state to ID mapping</li>
 *   <li>Dirty section tracking for updates</li>
 *   <li>Reference counting for safe cleanup</li>
 * </ul>
 */
public class WorldEngine implements Closeable {
    
    private final WorldIdentifier worldId;
    private final Path storagePath;
    private final AtomicInteger refCount = new AtomicInteger(0);
    
    private Mapper mapper;
    private ISectionDirtyCallback dirtyCallback;
    
    // TODO: Implement these components
    // private final StorageBackend storage;
    // private final ActiveSectionTracker sectionTracker;
    
    private volatile boolean isClosed = false;
    
    public WorldEngine(WorldIdentifier worldId) {
        this.worldId = worldId;
        this.storagePath = VoxyCommon.getDataPath().resolve(worldId.toPathSafe());
        
        Logger.info("WorldEngine created for: {} at {}", worldId, storagePath);
        
        // TODO: Initialize storage backend
        // TODO: Load mapper data
        // TODO: Set up section tracking
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
        if (dirtyCallback != null) {
            dirtyCallback.onSectionDirty(sectionPos);
        }
    }
    
    @Override
    public void close() {
        if (isClosed) return;
        isClosed = true;
        
        Logger.info("Closing WorldEngine for: {}", worldId);
        
        // TODO: Close storage backend
        // TODO: Flush pending writes
        // TODO: Save mapper data
        
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
