package me.cortex.neovoxy.common.world.storage;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks sections that are currently active or need updates.
 */
public class ActiveSectionTracker {

    // Set of sections that are currently "active" (loaded in memory/rendering)
    private final Set<Long> activeSections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // Set of sections that have been modified and need to be saved/re-meshed
    private final Set<Long> dirtySections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public ActiveSectionTracker() {
    }

    public void markActive(long sectionPos) {
        activeSections.add(sectionPos);
    }

    public void markInactive(long sectionPos) {
        activeSections.remove(sectionPos);
    }

    public boolean isActive(long sectionPos) {
        return activeSections.contains(sectionPos);
    }

    public void markDirty(long sectionPos) {
        dirtySections.add(sectionPos);
    }

    public void clearDirty(long sectionPos) {
        dirtySections.remove(sectionPos);
    }

    public boolean isDirty(long sectionPos) {
        return dirtySections.contains(sectionPos);
    }

    public Set<Long> getDirtySections() {
        return Collections.unmodifiableSet(dirtySections);
    }

    public Set<Long> getActiveSections() {
        return Collections.unmodifiableSet(activeSections);
    }

    /**
     * Clear all tracking data.
     */
    public void clear() {
        activeSections.clear();
        dirtySections.clear();
    }
}
