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
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = NeoVoxy.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class NeoVoxyDebugOverlay {
    
    @SubscribeEvent
    public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(
            ResourceLocation.fromNamespaceAndPath(NeoVoxy.MOD_ID, "debug_overlay"),
            NeoVoxyDebugOverlay::render
        );
    }
    
    private static void render(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        
        if (!mc.getDebugOverlay().showDebugScreen()) {
            return;
        }
        
        try {
            if (!NeoVoxyConfig.RENDER_STATISTICS.get()) {
                return;
            }
        } catch (IllegalStateException e) {
            return;
        }
        
        VoxyRenderSystem renderSystem = NeoVoxyClient.getActiveRenderSystem();
        List<String> lines = buildDebugInfo(renderSystem);
        
        Font font = mc.font;
        int y = 2;
        int screenWidth = graphics.guiWidth();
        
        for (String line : lines) {
            int width = font.width(line);
            int x = screenWidth - width - 2;
            graphics.fill(x - 1, y - 1, x + width + 1, y + font.lineHeight, 0x90505050);
            graphics.drawString(font, line, x, y, 0xFFFFFF, false);
            y += font.lineHeight + 1;
        }
    }
    
    private static List<String> buildDebugInfo(VoxyRenderSystem renderSystem) {
        List<String> lines = new ArrayList<>();
        lines.add("[NeoVoxy " + NeoVoxy.MOD_VERSION + "]");
        
        if (renderSystem == null) {
            lines.add("Render System: Not Active");
            return lines;
        }
        
        lines.add("Render Distance: " + renderSystem.getRenderDistance() + " sections");
        
        var nodeManager = renderSystem.getNodeManager();
        if (nodeManager != null) {
            lines.add("Nodes: " + nodeManager.getNodeCount());
        }
        
        var modelService = renderSystem.getModelService();
        if (modelService != null) {
            lines.add("Models: " + modelService.getStore().getModelCount());
        }
        
        lines.add("Status: Rendering");
        return lines;
    }
}
