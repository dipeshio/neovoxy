package me.cortex.neovoxy;

import me.cortex.neovoxy.client.config.NeoVoxyConfig;
import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.commonImpl.VoxyCommon;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * NeoVoxy - A NeoForge port of the Voxy LOD rendering engine.
 * 
 * <p>
 * Voxy is a high-performance Level of Detail (LOD) rendering system that
 * uses GPU-driven hierarchical occlusion culling and quad-based geometry
 * to render distant terrain far beyond vanilla render distance.
 * </p>
 */
@Mod(NeoVoxy.MOD_ID)
public class NeoVoxy {
    public static final String MOD_ID = "neovoxy";
    public static final String MOD_VERSION = "0.1.0.23-alpha";

    public NeoVoxy(IEventBus modEventBus, ModContainer modContainer) {
        // Load natives first thing
        try {
            me.cortex.neovoxy.common.util.NativeLoader.load();
        } catch (Exception e) {
            Logger.error("Failed to load NeoVoxy natives", e);
        }

        Logger.info("NeoVoxy " + MOD_VERSION + " initializing...");

        // Register configuration FIRST
        NeoVoxyConfig.register(modContainer);

        // Register lifecycle event handlers
        modEventBus.addListener(this::onCommonSetup);

        // Initialize common systems
        VoxyCommon.init();
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        Logger.info("NeoVoxy common setup complete");
    }
}
