package me.cortex.neovoxy.client.core;

import me.cortex.neovoxy.client.core.gl.GlShader;
import me.cortex.neovoxy.client.core.model.ModelBakerySubsystem;
import me.cortex.neovoxy.client.core.rendering.hierachical.AsyncNodeManager;
import me.cortex.neovoxy.client.core.rendering.hierachical.HierarchicalOcclusionTraverser;
import me.cortex.neovoxy.client.core.rendering.hierachical.NodeCleaner;
import me.cortex.neovoxy.client.core.rendering.util.SharedIndexBuffer;
import me.cortex.neovoxy.common.Logger;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * Normal render pipeline for vanilla Minecraft (no shader mods).
 * 
 * <p>
 * Uses standard OpenGL rendering with:
 * <ul>
 * <li>Multi-draw indirect for opaque geometry</li>
 * <li>Sorted draws for translucent geometry</li>
 * <li>Vanilla fog integration</li>
 * </ul>
 */
public class NormalRenderPipeline extends AbstractRenderPipeline {

    private GlShader quadShader;
    private GlShader translucentShader;

    private int vao;
    private boolean isInitialized = false;

    public NormalRenderPipeline(AsyncNodeManager nodeManager,
            NodeCleaner nodeCleaner,
            HierarchicalOcclusionTraverser traversal) {
        super(nodeManager, nodeCleaner, traversal);
    }

    /**
     * Initialize shaders and VAO.
     */
    public void initialize() {
        if (isInitialized)
            return;

        try {
            // Base defines for all shaders in this pipeline
            GlShader.Builder baseBuilder = new GlShader.Builder()
                    .define("QUAD_BUFFER_BINDING",
                            String.valueOf(me.cortex.neovoxy.client.core.gl.RenderBindings.QUAD_BUFFER_BINDING))
                    .define("MODEL_BUFFER_BINDING",
                            String.valueOf(me.cortex.neovoxy.client.core.gl.RenderBindings.MODEL_BUFFER_BINDING))
                    .define("MODEL_COLOUR_BUFFER_BINDING",
                            String.valueOf(me.cortex.neovoxy.client.core.gl.RenderBindings.MODEL_COLOUR_BUFFER_BINDING))
                    .define("POSITION_SCRATCH_BINDING",
                            String.valueOf(me.cortex.neovoxy.client.core.gl.RenderBindings.POSITION_SCRATCH_BINDING))
                    .define("LIGHTING_SAMPLER_BINDING",
                            String.valueOf(me.cortex.neovoxy.client.core.gl.RenderBindings.LIGHTING_SAMPLER_BINDING))
                    // Face tint values (Minecraft's directional shading)
                    .define("Z_AXIS_FACE_TINT", "0.8") // North/South faces
                    .define("X_AXIS_FACE_TINT", "0.6") // East/West faces
                    .define("UP_FACE_TINT", "1.0") // Top face
                    .define("DOWN_FACE_TINT", "0.5") // Bottom face
                    .define("NO_SHADE_FACE_TINT", "1.0"); // Unshaded

            // Load quad rendering shaders
            String vertSource = GlShader.Builder.loadSource("lod/gl46/quads3.vert");
            String fragSource = GlShader.Builder.loadSource("lod/gl46/quads.frag");

            quadShader = baseBuilder.copy()
                    .vertex(vertSource)
                    .fragment(fragSource)
                    .build();

            // Translucent shader with alpha blending
            translucentShader = baseBuilder.copy()
                    .vertex(vertSource)
                    .fragment(fragSource)
                    .define("TRANSLUCENT")
                    .build();

            // Create VAO (vertex attributes are generated in shader)
            vao = glCreateVertexArrays();

            isInitialized = true;
            Logger.info("NormalRenderPipeline initialized");

        } catch (Exception e) {
            Logger.error("Failed to initialize render pipeline", e);
        }
    }

    @Override
    public void setupExtraModelBakeryData(ModelBakerySubsystem modelBakery) {
        // Normal pipeline doesn't need extra data
    }

    @Override
    public void renderOpaque() {
        if (!isInitialized) {
            initialize();
            if (!isInitialized)
                return;
        }

        if (sectionRenderer == null)
            return;

        // Bind VAO and index buffer
        glBindVertexArray(vao);
        SharedIndexBuffer.INSTANCE.bind();

        // Use quad shader
        quadShader.use();

        // Opaque draw pass
        // The viewport is managed by VoxyRenderSystem and passed here or accessed via
        // traversal
        sectionRenderer.renderOpaque(traversal.getViewport());

        GlShader.unbind();
        glBindVertexArray(0);
    }

    @Override
    public void renderTranslucent() {
        if (!isInitialized || sectionRenderer == null)
            return;

        // Enable blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);

        // Bind VAO and index buffer
        glBindVertexArray(vao);
        SharedIndexBuffer.INSTANCE.bind();

        // Use translucent shader
        translucentShader.use();

        // Translucent draw pass (Sorted/Bucketed)
        sectionRenderer.renderTranslucent(traversal.getViewport());

        GlShader.unbind();
        glBindVertexArray(0);

        // Restore state
        glDepthMask(true);
        glDisable(GL_BLEND);
    }

    @Override
    public void postRender() {
        // Clean up any per-frame state
    }

    @Override
    public void close() {
        if (quadShader != null) {
            quadShader.close();
        }
        if (translucentShader != null) {
            translucentShader.close();
        }
        if (vao != 0) {
            glDeleteVertexArrays(vao);
        }
        isInitialized = false;
        Logger.info("NormalRenderPipeline closed");
    }
}
