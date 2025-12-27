package me.cortex.neovoxy.client.core;

import me.cortex.neovoxy.client.core.gl.GlShader;
import me.cortex.neovoxy.client.core.model.ModelBakerySubsystem;
import me.cortex.neovoxy.client.core.rendering.hierachical.AsyncNodeManager;
import me.cortex.neovoxy.client.core.rendering.hierachical.HierarchicalOcclusionTraverser;
import me.cortex.neovoxy.client.core.rendering.hierachical.NodeCleaner;
import me.cortex.neovoxy.client.core.rendering.util.SharedIndexBuffer;
import me.cortex.neovoxy.common.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * Normal render pipeline for vanilla Minecraft (no shader mods).
 * 
 * <p>Uses standard OpenGL rendering with:
 * <ul>
 *   <li>Multi-draw indirect for opaque geometry</li>
 *   <li>Sorted draws for translucent geometry</li>
 *   <li>Vanilla fog integration</li>
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
        if (isInitialized) return;
        
        try {
            // Load quad rendering shaders
            String vertSource = GlShader.Builder.loadSource("lod/gl46/quads3.vert");
            String fragSource = GlShader.Builder.loadSource("lod/gl46/quads.frag");
            
            quadShader = new GlShader.Builder()
                .vertex(vertSource)
                .fragment(fragSource)
                .build();
            
            // Translucent shader with alpha blending
            translucentShader = new GlShader.Builder()
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
            if (!isInitialized) return;
        }
        
        if (sectionRenderer == null) return;
        
        // Bind VAO and index buffer
        glBindVertexArray(vao);
        SharedIndexBuffer.INSTANCE.bind();
        
        // Use quad shader
        quadShader.use();
        
        // TODO: Set uniforms (MVP, camera position, etc.)
        
        // TODO: Bind section/geometry buffers
        
        // TODO: Execute multi-draw indirect
        
        GlShader.unbind();
        glBindVertexArray(0);
    }
    
    @Override
    public void renderTranslucent() {
        if (!isInitialized || sectionRenderer == null) return;
        
        // Enable blending
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        
        // Bind VAO and index buffer
        glBindVertexArray(vao);
        SharedIndexBuffer.INSTANCE.bind();
        
        // Use translucent shader
        translucentShader.use();
        
        // TODO: Set uniforms
        
        // TODO: Execute sorted draws for translucent geometry
        
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
