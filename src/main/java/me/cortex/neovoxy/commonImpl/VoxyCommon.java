package me.cortex.neovoxy.commonImpl;

import me.cortex.neovoxy.NeoVoxy;
import me.cortex.neovoxy.common.Logger;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Common singleton manager for Voxy instances.
 * Handles per-world/dimension instance creation and lifecycle.
 */
public final class VoxyCommon {
    public static final String MOD_VERSION = NeoVoxy.MOD_VERSION;
    
    private static boolean available = false;
    private static Function<WorldIdentifier, VoxyInstance> instanceFactory;
    private static final AtomicReference<VoxyInstance> currentInstance = new AtomicReference<>();
    
    // Special world flags
    public static boolean IS_MINE_IN_ABYSS = false;
    
    private VoxyCommon() {}
    
    /**
     * Initialize the common system.
     * Called from main mod constructor.
     */
    public static void init() {
        Logger.info("VoxyCommon initializing...");
        available = true;
    }
    
    /**
     * Check if Voxy is available on this system.
     */
    public static boolean isAvailable() {
        return available;
    }
    
    /**
     * Set the factory for creating Voxy instances.
     * Called from client setup after GPU capability check.
     */
    public static void setInstanceFactory(Function<WorldIdentifier, VoxyInstance> factory) {
        instanceFactory = factory;
        Logger.info("VoxyCommon instance factory registered");
    }
    
    /**
     * Get the current active Voxy instance.
     */
    public static VoxyInstance getInstance() {
        return currentInstance.get();
    }
    
    /**
     * Create or get an instance for the given world.
     */
    public static VoxyInstance getOrCreateInstance(WorldIdentifier worldId) {
        var existing = currentInstance.get();
        if (existing != null && existing.getWorldId().equals(worldId)) {
            return existing;
        }
        
        if (instanceFactory != null) {
            var newInstance = instanceFactory.apply(worldId);
            if (currentInstance.compareAndSet(existing, newInstance)) {
                if (existing != null) {
                    existing.close();
                }
                Logger.info("Created new Voxy instance for world: " + worldId);
                return newInstance;
            } else {
                newInstance.close();
                return currentInstance.get();
            }
        }
        return null;
    }
    
    /**
     * Close the current instance.
     */
    public static void closeInstance() {
        var instance = currentInstance.getAndSet(null);
        if (instance != null) {
            instance.close();
            Logger.info("Closed Voxy instance");
        }
    }
    
    /**
     * Get the base path for Voxy data storage.
     */
    public static Path getDataPath() {
        return FMLPaths.GAMEDIR.get().resolve("voxy");
    }
    
    /**
     * Get the config directory path.
     */
    public static Path getConfigPath() {
        return FMLPaths.CONFIGDIR.get();
    }
}
