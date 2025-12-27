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
    
    private final WorldEngine worldEngine;
    private final ServiceManager serviceManager;
    private VoxyRenderSystem renderSystem;
    
    public VoxyClientInstance(WorldIdentifier worldId) {
        super(worldId);
        
        Logger.info("Creating VoxyClientInstance for: " + worldId);
        
        // Initialize the service thread pool
        this.serviceManager = new ServiceManager(NeoVoxyConfig.SERVICE_THREADS.get());
        
        // Initialize the world engine for LOD storage
        this.worldEngine = new WorldEngine(worldId);
        
        Logger.info("VoxyClientInstance created successfully");
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
        
        if (!NeoVoxyConfig.isRenderingEnabled()) {
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
            return new Config(
                NeoVoxyConfig.SECTION_RENDER_DISTANCE.get(),
                NeoVoxyConfig.SERVICE_THREADS.get(),
                NeoVoxyConfig.SUBDIVISION_SIZE.get().floatValue(),
                NeoVoxyConfig.INGEST_ENABLED.get(),
                NeoVoxyConfig.ENABLE_RENDERING.get()
            );
        }
    }
}
