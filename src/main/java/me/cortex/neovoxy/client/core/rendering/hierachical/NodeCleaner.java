package me.cortex.neovoxy.client.core.rendering.hierachical;

import me.cortex.neovoxy.common.Logger;

/**
 * Cleans up stale nodes that haven't been rendered recently.
 * 
 * <p>Runs periodically to free GPU memory for nodes that are
 * no longer visible or needed.
 */
public class NodeCleaner {
    
    private final AsyncNodeManager nodeManager;
    
    private int cleanupFrameInterval = 60; // Clean every 60 frames
    private int frameCounter = 0;
    
    public NodeCleaner(AsyncNodeManager nodeManager) {
        this.nodeManager = nodeManager;
    }
    
    /**
     * Called each frame to potentially trigger cleanup.
     */
    public void tick(int currentFrame) {
        frameCounter++;
        
        if (frameCounter >= cleanupFrameInterval) {
            frameCounter = 0;
            performCleanup(currentFrame);
        }
    }
    
    /**
     * Force cleanup now.
     */
    public void forceCleanup(int currentFrame) {
        performCleanup(currentFrame);
        frameCounter = 0;
    }
    
    private void performCleanup(int currentFrame) {
        // TODO: Scan render tracker buffer for stale nodes
        // Nodes not rendered in the last N frames can have their
        // mesh data freed
        
        // This is done on GPU via compute shader in the original
    }
    
    /**
     * Set cleanup interval.
     */
    public void setCleanupInterval(int frames) {
        this.cleanupFrameInterval = Math.max(1, frames);
    }
}
