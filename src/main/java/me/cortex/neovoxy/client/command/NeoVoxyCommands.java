package me.cortex.neovoxy.client.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.cortex.neovoxy.NeoVoxy;
import me.cortex.neovoxy.NeoVoxyClient;
import me.cortex.neovoxy.client.config.NeoVoxyConfig;
import me.cortex.neovoxy.client.core.VoxyRenderSystem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

@EventBusSubscriber(modid = NeoVoxy.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class NeoVoxyCommands {
    
    @SubscribeEvent
    public static void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(buildCommand());
    }
    
    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        return Commands.literal("neovoxy")
            .then(Commands.literal("status")
                .executes(ctx -> { showStatus(ctx.getSource()); return 1; }))
            .then(Commands.literal("toggle")
                .executes(ctx -> { toggleRendering(ctx.getSource()); return 1; }))
            .then(Commands.literal("distance")
                .then(Commands.argument("sections", IntegerArgumentType.integer(4, 128))
                    .executes(ctx -> {
                        int distance = IntegerArgumentType.getInteger(ctx, "sections");
                        setRenderDistance(ctx.getSource(), distance);
                        return 1;
                    })))
            .then(Commands.literal("reload")
                .executes(ctx -> { reloadShaders(ctx.getSource()); return 1; }))
            .then(Commands.literal("stats")
                .executes(ctx -> { showStats(ctx.getSource()); return 1; }));
    }
    
    private static void showStatus(CommandSourceStack source) {
        VoxyRenderSystem rs = NeoVoxyClient.getActiveRenderSystem();
        
        if (!NeoVoxyClient.isSystemSupported()) {
            source.sendFailure(Component.literal("[NeoVoxy] Not supported on this system"));
            return;
        }
        
        boolean renderingEnabled = safeGetRenderingEnabled();
        
        if (rs == null) {
            source.sendSuccess(() -> Component.literal("[NeoVoxy] Render system not active"), false);
            source.sendSuccess(() -> Component.literal("  Rendering enabled: " + renderingEnabled), false);
        } else {
            source.sendSuccess(() -> Component.literal("[NeoVoxy] Render system active"), false);
            source.sendSuccess(() -> Component.literal("  Render Distance: " + rs.getRenderDistance() + " sections"), false);
        }
    }
    
    private static void toggleRendering(CommandSourceStack source) {
        boolean current = safeGetRenderingEnabled();
        
        if (current) {
            source.sendSuccess(() -> Component.literal("[NeoVoxy] Rendering is currently enabled"), false);
        } else {
            source.sendSuccess(() -> Component.literal("[NeoVoxy] Rendering is currently disabled"), false);
        }
        source.sendSuccess(() -> Component.literal("Edit config/neovoxy.toml to change settings"), false);
    }
    
    private static boolean safeGetRenderingEnabled() {
        try {
            return NeoVoxyConfig.ENABLE_RENDERING.get();
        } catch (IllegalStateException e) {
            return true; // Default
        }
    }
    
    private static void setRenderDistance(CommandSourceStack source, int distance) {
        VoxyRenderSystem rs = NeoVoxyClient.getActiveRenderSystem();
        
        if (rs == null) {
            source.sendFailure(Component.literal("[NeoVoxy] Render system not active"));
            return;
        }
        
        rs.setRenderDistance(distance);
        source.sendSuccess(() -> Component.literal("[NeoVoxy] Render distance set to " + distance + " sections"), false);
    }
    
    private static void reloadShaders(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("[NeoVoxy] Shader reload not yet implemented"), false);
    }
    
    private static void showStats(CommandSourceStack source) {
        VoxyRenderSystem rs = NeoVoxyClient.getActiveRenderSystem();
        
        if (rs == null) {
            source.sendFailure(Component.literal("[NeoVoxy] Render system not active"));
            return;
        }
        
        source.sendSuccess(() -> Component.literal("[NeoVoxy Statistics]"), false);
        source.sendSuccess(() -> Component.literal("  Render Distance: " + rs.getRenderDistance()), false);
        
        var nodeManager = rs.getNodeManager();
        if (nodeManager != null) {
            source.sendSuccess(() -> Component.literal("  Active Nodes: " + nodeManager.getNodeCount()), false);
        }
        
        var modelService = rs.getModelService();
        if (modelService != null) {
            source.sendSuccess(() -> Component.literal("  Loaded Models: " + modelService.getStore().getModelCount()), false);
        }
    }
}
