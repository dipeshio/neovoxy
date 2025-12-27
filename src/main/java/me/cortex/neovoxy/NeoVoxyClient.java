package me.cortex.neovoxy;

import me.cortex.neovoxy.client.VoxyClientInstance;
import me.cortex.neovoxy.client.config.NeoVoxyConfig;
import me.cortex.neovoxy.client.core.IGetVoxyRenderSystem;
import me.cortex.neovoxy.client.core.VoxyRenderSystem;
import me.cortex.neovoxy.client.core.gl.Capabilities;
import me.cortex.neovoxy.client.core.model.bakery.BudgetBufferRenderer;
import me.cortex.neovoxy.client.core.rendering.util.SharedIndexBuffer;
import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.commonImpl.VoxyCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayerManager;

/**
 * Client-side initialization for NeoVoxy.
 * Handles GPU capability detection, render system setup, and UI registration.
 */
@EventBusSubscriber(modid = NeoVoxy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class NeoVoxyClient {
    
    private static boolean systemSupported = false;
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        Logger.info("NeoVoxy client setup starting...");
        
        event.enqueueWork(() -> {
            initVoxyClient();
        });
    }
    
    /**
     * Initialize the Voxy rendering system.
     * Called during client setup after OpenGL context is available.
     */
    public static void initVoxyClient() {
        // Detect GPU capabilities
        Capabilities.init();
        
        if (Capabilities.INSTANCE.hasBrokenDepthSampler) {
            Logger.error("AMD broken depth sampler detected, NeoVoxy does not work correctly and has been disabled");
            return;
        }
        
        systemSupported = Capabilities.INSTANCE.compute 
                       && Capabilities.INSTANCE.indirectParameters 
                       && !Capabilities.INSTANCE.hasBrokenDepthSampler;
        
        if (systemSupported) {
            // Initialize shared resources
            SharedIndexBuffer.INSTANCE.id();
            BudgetBufferRenderer.init();
            
            // Set up the instance factory for world-per-dimension support
            VoxyCommon.setInstanceFactory(VoxyClientInstance::new);
            
            if (!Capabilities.INSTANCE.subgroup) {
                Logger.warn("GPU does not support subgroup operations, expect some performance degradation");
            }
            
            Logger.info("NeoVoxy rendering system initialized successfully");
        } else {
            Logger.error("NeoVoxy is unsupported on your system. Required: GL_ARB_compute_shader, GL_ARB_indirect_parameters");
        }
    }
    
    public static boolean isSystemSupported() {
        return systemSupported;
    }
    
    /**
     * Get the current VoxyRenderSystem if active.
     */
    public static VoxyRenderSystem getActiveRenderSystem() {
        var levelRenderer = Minecraft.getInstance().levelRenderer;
        if (levelRenderer instanceof IGetVoxyRenderSystem vrs) {
            return vrs.getVoxyRenderSystem();
        }
        return null;
    }
}
