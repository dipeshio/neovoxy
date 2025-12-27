package me.cortex.neovoxy.client.ui;

import me.cortex.neovoxy.NeoVoxy;
import me.cortex.neovoxy.NeoVoxyClient;
import me.cortex.neovoxy.client.config.NeoVoxyConfig;
import me.cortex.neovoxy.client.core.VoxyRenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayerManager;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Debug overlay for NeoVoxy statistics.
 * Shows LOD rendering info in F3 debug screen.
 */
@EventBusSubscriber(modid = NeoVoxy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class NeoVoxyDebugOverlay {
    
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        // Register our debug overlay
        event.registerAboveAll(
            ResourceLocation.fromNamespaceAndPath(NeoVoxy.MOD_ID, "debug_overlay"),
            NeoVoxyDebugOverlay::render
        );
    }
    
    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        
        // Only show in F3 debug mode with render statistics enabled
        if (!mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        
        if (!NeoVoxyConfig.RENDER_STATISTICS.get()) {
            return;
        }
        
        VoxyRenderSystem renderSystem = NeoVoxyClient.getActiveRenderSystem();
        
        List<String> lines = buildDebugInfo(renderSystem);
        
        // Render on right side of screen
        Font font = mc.font;
        int y = 2;
        int screenWidth = graphics.guiWidth();
        
        for (String line : lines) {
            int width = font.width(line);
            int x = screenWidth - width - 2;
            
            // Background
            graphics.fill(x - 1, y - 1, x + width + 1, y + font.lineHeight, 0x90505050);
            
            // Text
            graphics.drawString(font, line, x, y, 0xFFFFFF, false);
            
            y += font.lineHeight + 1;
        }
    }
    
    private static List<String> buildDebugInfo(VoxyRenderSystem renderSystem) {
        List<String> lines = new ArrayList<>();
        
        lines.add("§b[NeoVoxy " + NeoVoxy.MOD_VERSION + "]");
        
        if (renderSystem == null) {
            lines.add("§7Render System: §cNot Active");
            return lines;
        }
        
        lines.add("§7Render Distance: §f" + renderSystem.getRenderDistance() + " sections");
        
        // Node manager stats
        var nodeManager = renderSystem.getNodeManager();
        if (nodeManager != null) {
            lines.add("§7Nodes: §f" + nodeManager.getNodeCount());
        }
        
        // Model stats
        var modelService = renderSystem.getModelService();
        if (modelService != null) {
            lines.add("§7Models: §f" + modelService.getStore().getModelCount());
        }
        
        // Memory stats would go here
        lines.add("§7Status: §aRendering");
        
        return lines;
    }
}
