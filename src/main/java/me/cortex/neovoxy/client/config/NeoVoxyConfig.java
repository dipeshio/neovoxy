package me.cortex.neovoxy.client.config;

import me.cortex.neovoxy.NeoVoxy;
import me.cortex.neovoxy.common.Logger;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * NeoForge configuration for NeoVoxy.
 * Uses ModConfigSpec for type-safe, validated configuration.
 */
@EventBusSubscriber(modid = NeoVoxy.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class NeoVoxyConfig {

    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // General Settings
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue ENABLE_RENDERING;
    public static final ModConfigSpec.BooleanValue INGEST_ENABLED;

    // Performance Settings
    public static final ModConfigSpec.IntValue SECTION_RENDER_DISTANCE;
    public static final ModConfigSpec.IntValue SERVICE_THREADS;
    public static final ModConfigSpec.DoubleValue SUBDIVISION_SIZE;

    // Visual Settings
    public static final ModConfigSpec.BooleanValue USE_ENVIRONMENTAL_FOG;
    public static final ModConfigSpec.BooleanValue USE_RENDER_FOG;
    public static final ModConfigSpec.BooleanValue RENDER_STATISTICS;

    // Advanced Settings
    public static final ModConfigSpec.BooleanValue USE_EMBEDDIUM_THREADS;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("NeoVoxy Configuration")
                .push("general");

        ENABLED = BUILDER
                .comment("Master switch to enable/disable NeoVoxy entirely")
                .define("enabled", true);

        ENABLE_RENDERING = BUILDER
                .comment("Enable LOD rendering (can be toggled for debugging)")
                .define("enableRendering", true);

        INGEST_ENABLED = BUILDER
                .comment("Enable chunk-to-LOD conversion (voxelization)")
                .define("ingestEnabled", true);

        BUILDER.pop().push("performance");

        SECTION_RENDER_DISTANCE = BUILDER
                .comment("LOD render distance in sections (32 blocks each)")
                .defineInRange("sectionRenderDistance", 16, 4, 1500);

        SERVICE_THREADS = BUILDER
                .comment("Number of threads for background processing",
                        "Default is based on CPU core count")
                .defineInRange("serviceThreads", getDefaultThreadCount(), 1, 32);

        SUBDIVISION_SIZE = BUILDER
                .comment("Maximum screen-space size (pixels²) before subdividing to higher LOD",
                        "Lower = higher quality, higher = better performance")
                .defineInRange("subdivisionSize", 64.0, 16.0, 256.0);

        USE_EMBEDDIUM_THREADS = BUILDER
                .comment("Use Embeddium's builder threads for LOD generation",
                        "Can reduce stuttering at high render distances")
                .define("useEmbeddiumThreads", true);

        BUILDER.pop().push("visual");

        USE_ENVIRONMENTAL_FOG = BUILDER
                .comment("Enable environmental fog for distant terrain")
                .define("environmentalFog", true);

        USE_RENDER_FOG = BUILDER
                .comment("Enable render fog effect")
                .define("renderFog", true);

        RENDER_STATISTICS = BUILDER
                .comment("Show render statistics in F3 debug screen")
                .define("renderStatistics", true);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static int getDefaultThreadCount() {
        return Math.max(1, Runtime.getRuntime().availableProcessors() * 2 / 3);
    }

    /**
     * Register the config with the mod container.
     */
    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, SPEC, "neovoxy.toml");
        Logger.info("NeoVoxy config registered");
    }

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        if (event.getConfig().getSpec() == SPEC) {
            Logger.info("NeoVoxy config loaded");
        }
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == SPEC) {
            Logger.info("NeoVoxy config reloaded");
        }
    }

    /**
     * Check if rendering is currently enabled.
     * Returns true if config not loaded yet (safe default).
     */
    public static boolean isRenderingEnabled() {
        try {
            return ENABLED.get() && ENABLE_RENDERING.get();
        } catch (IllegalStateException e) {
            // Config not loaded yet, default to enabled
            return true;
        }
    }
}
