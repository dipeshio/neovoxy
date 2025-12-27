package me.cortex.neovoxy.commonImpl;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * Identifies a world/dimension for Voxy instance management.
 */
public record WorldIdentifier(
    String serverAddress,
    ResourceKey<Level> dimension
) {
    /**
     * Create identifier for a singleplayer world.
     */
    public static WorldIdentifier singleplayer(String worldName, ResourceKey<Level> dimension) {
        return new WorldIdentifier("local:" + worldName, dimension);
    }
    
    /**
     * Create identifier for a multiplayer world.
     */
    public static WorldIdentifier multiplayer(String serverAddress, ResourceKey<Level> dimension) {
        return new WorldIdentifier(serverAddress, dimension);
    }
    
    /**
     * Get a filesystem-safe name for this world.
     */
    public String toPathSafe() {
        String server = serverAddress.replaceAll("[^a-zA-Z0-9.-]", "_");
        String dim = dimension.location().toString().replaceAll("[^a-zA-Z0-9.-]", "_");
        return server + "/" + dim;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof WorldIdentifier that)) return false;
        return Objects.equals(serverAddress, that.serverAddress) 
            && Objects.equals(dimension, that.dimension);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(serverAddress, dimension);
    }
    
    @Override
    public String toString() {
        return serverAddress + " @ " + dimension.location();
    }
}
