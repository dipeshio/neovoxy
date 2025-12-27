package me.cortex.neovoxy.client.ui;

import me.cortex.neovoxy.NeoVoxy;
import me.cortex.neovoxy.NeoVoxyClient;
import me.cortex.neovoxy.client.config.NeoVoxyConfig;
import me.cortex.neovoxy.client.core.VoxyRenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class NeoVoxySettingsScreen extends Screen {
    
    private final Screen parent;
    
    public NeoVoxySettingsScreen(Screen parent) {
        super(Component.literal("NeoVoxy Settings"));
        this.parent = parent;
    }
    
    @Override
    protected void init() {
        int centerX = this.width / 2;
        int y = 60;
        
        // Title is drawn in render
        
        // Status info
        y += 40;
        
        // Distance buttons
        addRenderableWidget(Button.builder(Component.literal("Render Distance: " + getCurrentDistance()), 
            button -> cycleDistance(button))
            .bounds(centerX - 100, y, 200, 20)
            .build());
        
        y += 25;
        
        // Toggle rendering button
        addRenderableWidget(Button.builder(Component.literal("Rendering: " + (isRenderingEnabled() ? "ON" : "OFF")),
            button -> toggleRendering(button))
            .bounds(centerX - 100, y, 200, 20)
            .build());
        
        y += 25;
        
        // Stats button  
        addRenderableWidget(Button.builder(Component.literal("Show Statistics"),
            button -> showStats())
            .bounds(centerX - 100, y, 200, 20)
            .build());
        
        // Done button
        addRenderableWidget(Button.builder(Component.literal("Done"), 
            button -> onClose())
            .bounds(centerX - 100, this.height - 30, 200, 20)
            .build());
    }
    
    private int getCurrentDistance() {
        VoxyRenderSystem rs = NeoVoxyClient.getActiveRenderSystem();
        if (rs != null) {
            return rs.getRenderDistance();
        }
        try {
            return NeoVoxyConfig.SECTION_RENDER_DISTANCE.get();
        } catch (Exception e) {
            return 16;
        }
    }
    
    private void cycleDistance(Button button) {
        VoxyRenderSystem rs = NeoVoxyClient.getActiveRenderSystem();
        if (rs != null) {
            int current = rs.getRenderDistance();
            int next = current >= 64 ? 16 : current + 16;
            rs.setRenderDistance(next);
            button.setMessage(Component.literal("Render Distance: " + next));
        }
    }
    
    private boolean isRenderingEnabled() {
        try {
            return NeoVoxyConfig.isRenderingEnabled();
        } catch (Exception e) {
            return true;
        }
    }
    
    private void toggleRendering(Button button) {
        // Note: Can't modify config at runtime easily
        // Just show a message for now
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(Component.literal("Edit config/neovoxy.toml to toggle"));
        }
    }
    
    private void showStats() {
        VoxyRenderSystem rs = NeoVoxyClient.getActiveRenderSystem();
        if (minecraft != null && minecraft.player != null) {
            if (rs == null) {
                minecraft.player.sendSystemMessage(Component.literal("[NeoVoxy] Render system not active"));
            } else {
                minecraft.player.sendSystemMessage(Component.literal("[NeoVoxy] Distance: " + rs.getRenderDistance()));
                var nm = rs.getNodeManager();
                if (nm != null) {
                    minecraft.player.sendSystemMessage(Component.literal("[NeoVoxy] Nodes: " + nm.getNodeCount()));
                }
            }
        }
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        
        // Title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Status
        String status;
        VoxyRenderSystem rs = NeoVoxyClient.getActiveRenderSystem();
        if (!NeoVoxyClient.isSystemSupported()) {
            status = "Status: NOT SUPPORTED";
        } else if (rs == null) {
            status = "Status: NOT ACTIVE";
        } else {
            status = "Status: ACTIVE";
        }
        graphics.drawCenteredString(this.font, status, this.width / 2, 45, rs != null ? 0x55FF55 : 0xFFAA00);
        
        super.render(graphics, mouseX, mouseY, partialTick);
    }
    
    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }
    
    public static void open() {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new NeoVoxySettingsScreen(mc.screen));
    }
}
