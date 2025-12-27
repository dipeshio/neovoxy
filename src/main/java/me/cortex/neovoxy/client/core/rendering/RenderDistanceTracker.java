package me.cortex.neovoxy.client.core.rendering;

import me.cortex.neovoxy.common.Logger;

import java.util.function.BiConsumer;

/**
 * Tracks render distance and manages top-level LOD node creation/removal.
 * 
 * <p>Divides the world into "sections" around the player and ensures
 * LOD nodes exist for visible regions.
 */
public class RenderDistanceTracker {
    
    private final int sectionSize; // Size of each section in blocks
    private final int minSectionY;
    private final int maxSectionY;
    
    private final BiConsumer<Integer, Integer> addCallback;
    private final BiConsumer<Integer, Integer> removeCallback;
    
    private int centerX = Integer.MIN_VALUE;
    private int centerZ = Integer.MIN_VALUE;
    private int renderDistance;
    
    // Tracked sections (simple bit set would be more efficient)
    private boolean[][] trackedSections;
    private int trackingSize;
    
    public RenderDistanceTracker(int sectionSize, int minSectionY, int maxSectionY,
                                  BiConsumer<Integer, Integer> addCallback,
                                  BiConsumer<Integer, Integer> removeCallback) {
        this.sectionSize = sectionSize;
        this.minSectionY = minSectionY;
        this.maxSectionY = maxSectionY;
        this.addCallback = addCallback;
        this.removeCallback = removeCallback;
        this.renderDistance = 16;
        
        updateTrackingGrid();
    }
    
    /**
     * Set render distance in sections.
     */
    public void setRenderDistance(int distance) {
        if (this.renderDistance != distance) {
            this.renderDistance = distance;
            updateTrackingGrid();
            
            // Force full recalculation
            centerX = Integer.MIN_VALUE;
        }
    }
    
    private void updateTrackingGrid() {
        trackingSize = renderDistance * 2 + 1;
        trackedSections = new boolean[trackingSize][trackingSize];
    }
    
    /**
     * Update tracking based on player position.
     * 
     * @param playerX Player X position
     * @param playerZ Player Z position
     */
    public void update(double playerX, double playerZ) {
        int newCenterX = (int) Math.floor(playerX) >> 5; // /32 for section coords
        int newCenterZ = (int) Math.floor(playerZ) >> 5;
        
        if (newCenterX == centerX && newCenterZ == centerZ) {
            return; // No change
        }
        
        // Handle movement
        if (centerX == Integer.MIN_VALUE) {
            // First update - add all sections in range
            centerX = newCenterX;
            centerZ = newCenterZ;
            addAllInRange();
        } else {
            // Incremental update
            int deltaX = newCenterX - centerX;
            int deltaZ = newCenterZ - centerZ;
            
            if (Math.abs(deltaX) > renderDistance || Math.abs(deltaZ) > renderDistance) {
                // Moved too far - do full reset
                removeAllTracked();
                centerX = newCenterX;
                centerZ = newCenterZ;
                addAllInRange();
            } else {
                // Incremental move
                incrementalUpdate(deltaX, deltaZ);
                centerX = newCenterX;
                centerZ = newCenterZ;
            }
        }
    }
    
    private void addAllInRange() {
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                
                addCallback.accept(x, z);
                
                int idx = dx + renderDistance;
                int idz = dz + renderDistance;
                if (idx >= 0 && idx < trackingSize && idz >= 0 && idz < trackingSize) {
                    trackedSections[idx][idz] = true;
                }
            }
        }
    }
    
    private void removeAllTracked() {
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int idx = dx + renderDistance;
                int idz = dz + renderDistance;
                
                if (idx >= 0 && idx < trackingSize && idz >= 0 && idz < trackingSize) {
                    if (trackedSections[idx][idz]) {
                        removeCallback.accept(centerX + dx, centerZ + dz);
                        trackedSections[idx][idz] = false;
                    }
                }
            }
        }
    }
    
    private void incrementalUpdate(int deltaX, int deltaZ) {
        // Remove sections that will be out of range
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int newDx = dx - deltaX;
                int newDz = dz - deltaZ;
                
                // Check if this section will be out of range after move
                if (Math.abs(newDx) > renderDistance || Math.abs(newDz) > renderDistance) {
                    int idx = dx + renderDistance;
                    int idz = dz + renderDistance;
                    
                    if (idx >= 0 && idx < trackingSize && idz >= 0 && idz < trackingSize) {
                        if (trackedSections[idx][idz]) {
                            removeCallback.accept(centerX + dx, centerZ + dz);
                        }
                    }
                }
            }
        }
        
        // Shift tracking grid
        boolean[][] newTracked = new boolean[trackingSize][trackingSize];
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int oldDx = dx + deltaX;
                int oldDz = dz + deltaZ;
                
                int idx = dx + renderDistance;
                int idz = dz + renderDistance;
                int oldIdx = oldDx + renderDistance;
                int oldIdz = oldDz + renderDistance;
                
                if (oldIdx >= 0 && oldIdx < trackingSize && oldIdz >= 0 && oldIdz < trackingSize) {
                    newTracked[idx][idz] = trackedSections[oldIdx][oldIdz];
                }
            }
        }
        trackedSections = newTracked;
        
        // Add new sections that are now in range
        for (int dx = -renderDistance; dx <= renderDistance; dx++) {
            for (int dz = -renderDistance; dz <= renderDistance; dz++) {
                int idx = dx + renderDistance;
                int idz = dz + renderDistance;
                
                if (!trackedSections[idx][idz]) {
                    addCallback.accept(centerX + deltaX + dx, centerZ + deltaZ + dz);
                    trackedSections[idx][idz] = true;
                }
            }
        }
    }
    
    /**
     * Get current render distance.
     */
    public int getRenderDistance() {
        return renderDistance;
    }
}
