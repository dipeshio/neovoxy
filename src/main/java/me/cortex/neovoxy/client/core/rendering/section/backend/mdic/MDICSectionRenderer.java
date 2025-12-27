package me.cortex.neovoxy.client.core.rendering.section.backend.mdic;

import me.cortex.neovoxy.client.core.AbstractRenderPipeline;
import me.cortex.neovoxy.client.core.gl.GlBuffer;
import me.cortex.neovoxy.client.core.model.ModelStore;
import me.cortex.neovoxy.client.core.rendering.section.backend.AbstractSectionRenderer;
import me.cortex.neovoxy.client.core.rendering.section.geometry.IGeometryData;
import me.cortex.neovoxy.client.core.rendering.Viewport;
import me.cortex.neovoxy.common.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL46.*;

/**
 * Multi-Draw Indirect Count (MDIC) section renderer.
 * 
 * <p>
 * Uses glMultiDrawElementsIndirectCount for efficient batched rendering
 * of LOD sections with GPU-generated draw commands.
 */
public class MDICSectionRenderer extends AbstractSectionRenderer {

    // Draw command buffer
    private GlBuffer drawCommandBuffer;
    private GlBuffer drawCountBuffer;

    // Section metadata buffer
    private GlBuffer sectionBuffer;

    private boolean isInitialized = false;

    public MDICSectionRenderer(AbstractRenderPipeline pipeline,
            ModelStore modelStore,
            IGeometryData geometryData) {
        super(pipeline, modelStore, geometryData);
    }

    /**
     * Initialize buffers for MDIC rendering.
     */
    public void initialize() {
        if (isInitialized)
            return;

        int maxDraws = 1 << 16; // 65536 draw commands
        int maxSections = 1 << 20; // ~1M sections

        // DrawElementsIndirectCommand: count, instanceCount, firstIndex, baseVertex,
        // baseInstance
        // = 5 * 4 = 20 bytes per command
        drawCommandBuffer = new GlBuffer((long) maxDraws * 20,
                org.lwjgl.opengl.GL44.GL_DYNAMIC_STORAGE_BIT);

        // Draw count buffer: holds the number of draw commands
        drawCountBuffer = new GlBuffer(16, // Enough for dispatch + counts
                org.lwjgl.opengl.GL44.GL_DYNAMIC_STORAGE_BIT);

        // Section metadata (32 bytes per section)
        sectionBuffer = new GlBuffer((long) maxSections * 32,
                org.lwjgl.opengl.GL44.GL_DYNAMIC_STORAGE_BIT);

        isInitialized = true;
        Logger.info("MDICSectionRenderer initialized");
    }

    @Override
    public Viewport createViewport() {
        return new Viewport();
    }

    @Override
    public void renderOpaque(Viewport viewport) {
        if (!isInitialized) {
            initialize();
            if (!isInitialized)
                return;
        }

        // Bind buffers using centralized RenderBindings
        drawCommandBuffer.bindBase(GL_SHADER_STORAGE_BUFFER,
                me.cortex.neovoxy.client.core.gl.RenderBindings.DRAW_BUFFER_BINDING);
        drawCountBuffer.bindBase(GL_SHADER_STORAGE_BUFFER,
                me.cortex.neovoxy.client.core.gl.RenderBindings.DRAW_COUNT_BUFFER_BINDING);
        sectionBuffer.bindBase(GL_SHADER_STORAGE_BUFFER,
                me.cortex.neovoxy.client.core.gl.RenderBindings.SECTION_METADATA_BUFFER_BINDING);
        modelStore.bind(me.cortex.neovoxy.client.core.gl.RenderBindings.MODEL_BUFFER_BINDING,
                me.cortex.neovoxy.client.core.gl.RenderBindings.MODEL_COLOUR_BUFFER_BINDING);

        // Bind geometry quad buffer
        glBindBufferBase(GL_SHADER_STORAGE_BUFFER, me.cortex.neovoxy.client.core.gl.RenderBindings.QUAD_BUFFER_BINDING,
                geometryData.getQuadBufferId());

        // Use MDIC for batched rendering
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, drawCommandBuffer.id());
        glBindBuffer(GL_PARAMETER_BUFFER, drawCountBuffer.id());

        // Draw indexed primitives with indirect count
        // The count of draw commands is read from the parameter buffer
        glMultiDrawElementsIndirectCount(
                GL_TRIANGLES, // mode
                GL_UNSIGNED_INT, // index type
                0, // indirect buffer offset (DrawCommand array start)
                12, // parameter buffer offset (opaqueDrawCount is at byte 12? No, check
                    // bindings.glsl)
                65536, // max draw count
                0 // stride (0 = tightly packed)
        );

        // Unbind
        glBindBuffer(GL_DRAW_INDIRECT_BUFFER, 0);
        glBindBuffer(GL_PARAMETER_BUFFER, 0);
    }

    @Override
    public void renderTranslucent(Viewport viewport) {
        if (!isInitialized)
            return;

        // Similar to opaque but with sorted draw order
        // TODO: Use translucent draw command section of buffer
    }

    @Override
    public void close() {
        if (drawCommandBuffer != null) {
            drawCommandBuffer.close();
        }
        if (drawCountBuffer != null) {
            drawCountBuffer.close();
        }
        if (sectionBuffer != null) {
            sectionBuffer.close();
        }
        isInitialized = false;
        Logger.info("MDICSectionRenderer closed");
    }

    /**
     * Factory for creating MDIC renderers.
     */
    public static final Factory<MDICSectionRenderer, IGeometryData> FACTORY = new Factory<>() {
        @Override
        public MDICSectionRenderer create(AbstractRenderPipeline pipeline,
                ModelStore modelStore,
                IGeometryData geometryData) {
            return new MDICSectionRenderer(pipeline, modelStore, geometryData);
        }

        @Override
        public Class<MDICSectionRenderer> clz() {
            return MDICSectionRenderer.class;
        }
    };
}
