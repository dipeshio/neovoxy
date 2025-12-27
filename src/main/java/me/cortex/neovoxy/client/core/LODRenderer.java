package me.cortex.neovoxy.client.core;

import me.cortex.neovoxy.NeoVoxy;
import me.cortex.neovoxy.NeoVoxyClient;
import me.cortex.neovoxy.client.config.NeoVoxyConfig;
import me.cortex.neovoxy.common.Logger;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

/**
 * LOD Renderer that hooks into NeoForge render events.
 * 
 * <p>Injects LOD draw calls after solid blocks are rendered but before
 * translucent geometry, ensuring proper depth integration with vanilla terrain.
 */
@EventBusSubscriber(modid = NeoVoxy.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class LODRenderer {
    
    /**
     * Main render event handler.
     * Called at various stages during level rendering.
     */
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        // Only render after solid blocks, before translucent
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS) {
            return;
        }
        
        if (!NeoVoxyConfig.isRenderingEnabled()) {
            return;
        }
        
        VoxyRenderSystem renderSystem = NeoVoxyClient.getActiveRenderSystem();
        if (renderSystem == null) {
            return;
        }
        
        try {
            // Extract camera and matrices from event
            Camera camera = event.getCamera();
            Matrix4f projection = new Matrix4f(event.getProjectionMatrix());
            Matrix4f modelView = new Matrix4f(event.getModelViewMatrix());
            
            double cameraX = camera.getPosition().x;
            double cameraY = camera.getPosition().y;
            double cameraZ = camera.getPosition().z;
            
            // Set up the viewport with camera data
            renderSystem.setupViewport(projection, modelView, cameraX, cameraY, cameraZ);
            
            // Render LOD terrain
            renderSystem.render();
            
        } catch (Exception e) {
            Logger.error("Error during LOD rendering", e);
        }
    }
}
