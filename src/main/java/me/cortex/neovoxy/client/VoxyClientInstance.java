package me.cortex.neovoxy.client;

import me.cortex.neovoxy.client.config.NeoVoxyConfig;
import me.cortex.neovoxy.client.core.IGetVoxyRenderSystem;
import me.cortex.neovoxy.client.core.VoxyRenderSystem;
import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.common.thread.ServiceManager;
import me.cortex.neovoxy.common.world.WorldEngine;
import me.cortex.neovoxy.commonImpl.VoxyInstance;
import me.cortex.neovoxy.commonImpl.WorldIdentifier;
import net.minecraft.client.Minecraft;

/**
 * Client-side Voxy instance managing rendering for a specific world.
 */
public class VoxyClientInstance extends VoxyInstance {
    
    private static final int DEFAULT_SERVICE_THREADS = 4;
    
    private final WorldEngine worldEngine;
    private final ServiceManager serviceManager;
    private VoxyRenderSystem renderSystem;
    
    public VoxyClientInstance(WorldIdentifier worldId) {
        super(worldId);
        
        Logger.info("Creating VoxyClientInstance for: " + worldId);
        
        // Initialize the service thread pool - use default if config not loaded yet
        int threadCount = getServiceThreadCount();
        this.serviceManager = new ServiceManager(threadCount);
        
        // Initialize the world engine for LOD storage
        this.worldEngine = new WorldEngine(worldId);
        
        Logger.info("VoxyClientInstance created successfully with {} service threads", threadCount);
    }
    
    /**
     * Safely get service thread count, using default if config not loaded.
     */
    private static int getServiceThreadCount() {
        try {
            return NeoVoxyConfig.SERVICE_THREADS.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet, use default
            Logger.info("Config not loaded yet, using default service thread count: {}", DEFAULT_SERVICE_THREADS);
            return DEFAULT_SERVICE_THREADS;
        }
    }
    
    /**
     * Create and attach the render system to the level renderer.
     * Called when the level renderer is ready.
     */
    public void createRenderSystem() {
        if (renderSystem != null) {
            Logger.warn("RenderSystem already exists, skipping creation");
            return;
        }
        
        if (!isRenderingEnabled()) {
            Logger.info("Rendering disabled in config, skipping RenderSystem creation");
            return;
        }
        
        try {
            this.renderSystem = new VoxyRenderSystem(worldEngine, serviceManager);
            Logger.info("VoxyRenderSystem created");
        } catch (Exception e) {
            Logger.error("Failed to create VoxyRenderSystem", e);
        }
    }
    
    /**
     * Safely check if rendering is enabled.
     */
    private static boolean isRenderingEnabled() {
        try {
            return NeoVoxyConfig.isRenderingEnabled();
        } catch (IllegalStateException e) {
            // Config not loaded yet, default to enabled
            return true;
        }
    }
    
    /**
     * Get the render system if available.
     */
    public VoxyRenderSystem getRenderSystem() {
        return renderSystem;
    }
    
    /**
     * Get the world engine.
     */
    public WorldEngine getWorldEngine() {
        return worldEngine;
    }
    
    @Override
    public void tick() {
        if (renderSystem != null) {
            // Perform per-tick updates (e.g., update render distance tracker)
            // renderSystem.tick();
        }
    }
    
    @Override
    public void close() {
        Logger.info("Closing VoxyClientInstance for: " + worldId);
        
        if (renderSystem != null) {
            try {
                renderSystem.close();
            } catch (Exception e) {
                Logger.error("Error closing RenderSystem", e);
            }
            renderSystem = null;
        }
        
        if (worldEngine != null) {
            try {
                worldEngine.close();
            } catch (Exception e) {
                Logger.error("Error closing WorldEngine", e);
            }
        }
        
        if (serviceManager != null) {
            try {
                serviceManager.shutdown();
            } catch (Exception e) {
                Logger.error("Error shutting down ServiceManager", e);
            }
        }
        
        Logger.info("VoxyClientInstance closed");
    }
    
    /**
     * Config record for client instance settings.
     */
    public record Config(
        int renderDistance,
        int serviceThreads,
        float subdivisionSize,
        boolean ingestEnabled,
        boolean renderingEnabled
    ) {
        public static Config fromNeoVoxyConfig() {
            try {
                return new Config(
                    NeoVoxyConfig.SECTION_RENDER_DISTANCE.get(),
                    NeoVoxyConfig.SERVICE_THREADS.get(),
                    NeoVoxyConfig.SUBDIVISION_SIZE.get().floatValue(),
                    NeoVoxyConfig.INGEST_ENABLED.get(),
                    NeoVoxyConfig.ENABLE_RENDERING.get()
                );
            } catch (IllegalStateException e) {
                // Config not loaded yet, use defaults
                return new Config(32, DEFAULT_SERVICE_THREADS, 16.0f, true, true);
            }
        }
    }
}
