package me.cortex.neovoxy.client.core;

/**
 * Interface for accessing the VoxyRenderSystem from LevelRenderer.
 * Implemented via mixin.
 */
public interface IGetVoxyRenderSystem {
    /**
     * Get the VoxyRenderSystem attached to this level renderer.
     * @return The render system, or null if not initialized
     */
    VoxyRenderSystem getVoxyRenderSystem();
    
    /**
     * Set the VoxyRenderSystem for this level renderer.
     */
    void setVoxyRenderSystem(VoxyRenderSystem system);
}
