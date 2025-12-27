package me.cortex.neovoxy.client.core.gl;

/**
 * Centralized OpenGL binding points for the NeoVoxy rendering system.
 * 
 * <p>
 * These constants must match the binding locations defined in the GLSL shaders
 * (see bindings.glsl).
 */
public final class RenderBindings {

    // Uniform Buffer Objects (UBO)
    public static final int SCENE_UNIFORM_BINDING = 0;

    // Shader Storage Buffer Objects (SSBO)
    public static final int NODE_BUFFER_BINDING = 1;
    public static final int VISIBILITY_BUFFER_BINDING = 2;
    public static final int RENDER_TRACKER_BINDING = 3;

    public static final int QUAD_BUFFER_BINDING = 4;
    public static final int SECTION_METADATA_BUFFER_BINDING = 5;

    public static final int DRAW_BUFFER_BINDING = 6;
    public static final int DRAW_COUNT_BUFFER_BINDING = 7;

    public static final int MODEL_BUFFER_BINDING = 8;
    public static final int MODEL_COLOUR_BUFFER_BINDING = 9;

    public static final int POSITION_SCRATCH_BINDING = 10;

    // Sampler Bindings
    public static final int BLOCK_MODEL_TEXTURE_BINDING = 0;
    public static final int LIGHTING_SAMPLER_BINDING = 1;
    public static final int HIZ_SAMPLER_BINDING = 2;

    private RenderBindings() {
    }
}
