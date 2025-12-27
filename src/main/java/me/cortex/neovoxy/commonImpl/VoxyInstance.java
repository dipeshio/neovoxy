package me.cortex.neovoxy.commonImpl;

import java.io.Closeable;

/**
 * Base class for a Voxy instance tied to a specific world.
 * Each world/dimension gets its own instance with separate storage and render state.
 */
public abstract class VoxyInstance implements Closeable {
    protected final WorldIdentifier worldId;
    
    protected VoxyInstance(WorldIdentifier worldId) {
        this.worldId = worldId;
    }
    
    public WorldIdentifier getWorldId() {
        return worldId;
    }
    
    /**
     * Called on each client tick.
     */
    public abstract void tick();
    
    /**
     * Close and clean up all resources.
     */
    @Override
    public abstract void close();
}
