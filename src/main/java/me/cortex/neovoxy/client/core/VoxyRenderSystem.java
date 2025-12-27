package me.cortex.neovoxy.client.core;

import me.cortex.neovoxy.client.config.NeoVoxyConfig;
import me.cortex.neovoxy.client.core.gl.Capabilities;
import me.cortex.neovoxy.client.core.gl.GlBuffer;
import me.cortex.neovoxy.client.core.model.ModelBakerySubsystem;
import me.cortex.neovoxy.client.core.rendering.Viewport;
import me.cortex.neovoxy.client.core.rendering.hierachical.AsyncNodeManager;
import me.cortex.neovoxy.client.core.rendering.hierachical.HierarchicalOcclusionTraverser;
import me.cortex.neovoxy.client.core.rendering.hierachical.NodeCleaner;
import me.cortex.neovoxy.client.core.rendering.hierachical.RenderGenerationService;
import me.cortex.neovoxy.client.core.rendering.section.IUsesMeshlets;
import me.cortex.neovoxy.client.core.rendering.section.backend.AbstractSectionRenderer;
import me.cortex.neovoxy.client.core.rendering.section.backend.mdic.MDICSectionRenderer;
import me.cortex.neovoxy.client.core.rendering.section.geometry.BasicSectionGeometryData;
import me.cortex.neovoxy.client.core.rendering.section.geometry.IGeometryData;
import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.common.thread.ServiceManager;
import me.cortex.neovoxy.common.world.WorldEngine;
import me.cortex.neovoxy.common.world.other.Mapper;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;

import java.io.Closeable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.*;

/**
 * Core rendering system for Voxy LOD rendering.
 * 
 * <p>Manages the complete rendering pipeline including:
 * <ul>
 *   <li>Hierarchical occlusion traversal (GPU-driven)</li>
 *   <li>Section geometry data and mesh generation</li>
 *   <li>Model bakery for block state → quad data</li>
 *   <li>Render pipeline selection (Normal/Oculus)</li>
 * </ul>
 */
public class VoxyRenderSystem implements Closeable {
    
    // Default geometry buffer size: 512 MB
    private static final long DEFAULT_GEOMETRY_CAPACITY = 512L * 1024 * 1024;
    
    private final WorldEngine worldEngine;
    private final ServiceManager serviceManager;
    
    // Subsystems
    private final ModelBakerySubsystem modelService;
    private final RenderGenerationService renderGen;
    private final IGeometryData geometryData;
    private final AsyncNodeManager nodeManager;
    private final NodeCleaner nodeCleaner;
    private final HierarchicalOcclusionTraverser traversal;
    private final AbstractRenderPipeline pipeline;
    private final AbstractSectionRenderer sectionRenderer;
    
    // Uniform buffer for scene data
    private GlBuffer sceneUniformBuffer;
    
    // Current viewport
    private final Viewport viewport = new Viewport();
    
    private final AtomicInteger frameId = new AtomicInteger(0);
    private int renderDistance;
    private boolean isInitialized = false;
    
    public VoxyRenderSystem(WorldEngine world, ServiceManager sm) {
        Logger.info("Creating VoxyRenderSystem...");
        
        // Keep world reference
        world.acquireRef();
        this.worldEngine = world;
        this.serviceManager = sm;
        
        // Save current GL state
        int[] oldBufferBindings = new int[10];
        for (int i = 0; i < oldBufferBindings.length; i++) {
            oldBufferBindings[i] = glGetIntegeri(GL_SHADER_STORAGE_BUFFER_BINDING, i);
        }
        
        try {
            // Wait for OpenGL to finish any pending operations
            glFinish();
            
            // Initialize all subsystems
            Mapper mapper = new Mapper();
            world.setMapper(mapper);
            
            // Model bakery for block state -> quad conversion
            this.modelService = new ModelBakerySubsystem(mapper);
            
            // Geometry storage on GPU
            this.geometryData = new BasicSectionGeometryData(1 << 20, DEFAULT_GEOMETRY_CAPACITY);
            
            // Render generation service (background mesh building)
            this.renderGen = new RenderGenerationService(world, modelService, sm, false);
            
            // Hierarchical node management
            this.nodeManager = new AsyncNodeManager(1 << 21, geometryData, renderGen);
            this.nodeCleaner = new NodeCleaner(nodeManager);
            
            // GPU-driven traversal
            this.traversal = new HierarchicalOcclusionTraverser(nodeManager, nodeCleaner, renderGen);
            
            // Dirty callback for world updates
            world.setDirtyCallback(nodeManager::worldEvent);
            
            // Create render pipeline
            this.pipeline = RenderPipelineFactory.createPipeline(
                nodeManager, nodeCleaner, traversal, this::frexStillHasWork);
            
            // Create section renderer
            this.sectionRenderer = MDICSectionRenderer.FACTORY.create(
                pipeline, modelService.getStore(), geometryData);
            pipeline.setSectionRenderer(sectionRenderer);
            
            // Scene uniform buffer (MVP, camera pos, frame ID, etc.)
            this.sceneUniformBuffer = new GlBuffer(256, GL_DYNAMIC_STORAGE_BIT);
            
            // Start services
            nodeManager.start();
            renderGen.start();
            
            this.renderDistance = NeoVoxyConfig.SECTION_RENDER_DISTANCE.get();
            this.isInitialized = true;
            
            Logger.info("VoxyRenderSystem created with {} MB geometry capacity", 
                        DEFAULT_GEOMETRY_CAPACITY / (1024 * 1024));
            
        } catch (RuntimeException e) {
            world.releaseRef();
            throw e;
        } finally {
            // Restore GL state
            for (int i = 0; i < oldBufferBindings.length; i++) {
                glBindBufferBase(GL_SHADER_STORAGE_BUFFER, i, oldBufferBindings[i]);
            }
        }
    }
    
    private boolean frexStillHasWork() {
        // For FREX flawless frames support
        return renderGen.getPendingCount() > 0;
    }
    
    /**
     * Set up the viewport for rendering.
     * Called before render() with camera matrices.
     */
    public void setupViewport(Matrix4f projection, Matrix4f modelView, 
                              double cameraX, double cameraY, double cameraZ) {
        if (!isInitialized || !NeoVoxyConfig.isRenderingEnabled()) {
            return;
        }
        
        int[] dims = new int[4];
        glGetIntegerv(GL_VIEWPORT, dims);
        
        viewport.setProjection(projection)
                .setModelView(modelView)
                .setCameraPosition(cameraX, cameraY, cameraZ)
                .setDimensions(dims[2], dims[3]);
        
        // Update scene uniform buffer
        updateSceneUniforms();
    }
    
    private void updateSceneUniforms() {
        // Pack uniform data
        float[] mvpData = new float[16];
        viewport.getMVP().get(mvpData);
        
        // MVP matrix (64 bytes)
        // Camera section pos (12 bytes) 
        // Frame ID (4 bytes)
        // Camera sub-pos (12 bytes)
        // ... etc
        
        java.nio.ByteBuffer buffer = org.lwjgl.BufferUtils.createByteBuffer(256);
        buffer.asFloatBuffer().put(mvpData);
        buffer.position(64);
        
        // Camera section position (integer section coords)
        int secX = (int) Math.floor(viewport.getCameraX()) >> 5;
        int secY = (int) Math.floor(viewport.getCameraY()) >> 5;
        int secZ = (int) Math.floor(viewport.getCameraZ()) >> 5;
        buffer.putInt(secX).putInt(secY).putInt(secZ);
        buffer.putInt(frameId.get());
        
        // Camera sub-section position (float offset within section)
        buffer.putFloat((float)(viewport.getCameraX() - (secX << 5)));
        buffer.putFloat((float)(viewport.getCameraY() - (secY << 5)));
        buffer.putFloat((float)(viewport.getCameraZ() - (secZ << 5)));
        
        buffer.flip();
        sceneUniformBuffer.upload(0, buffer);
    }
    
    /**
     * Render the LOD terrain.
     * Called from RenderLevelStageEvent.
     */
    public void render() {
        if (!isInitialized || !NeoVoxyConfig.isRenderingEnabled()) {
            return;
        }
        
        int frame = frameId.incrementAndGet();
        
        // Bind scene uniforms
        sceneUniformBuffer.bindBase(GL_UNIFORM_BUFFER, 0);
        
        // Run hierarchical traversal
        traversal.traverse(frame, viewport.getWidth(), viewport.getHeight());
        
        // Tick node cleaner
        nodeCleaner.tick(frame);
        
        // Render opaque geometry
        pipeline.renderOpaque();
        
        // Render translucent geometry
        pipeline.renderTranslucent();
        
        // Post-render cleanup
        pipeline.postRender();
    }
    
    /**
     * Set the LOD render distance.
     */
    public void setRenderDistance(int distance) {
        this.renderDistance = distance;
        // Update render distance tracker would go here
    }
    
    /**
     * Get current render distance.
     */
    public int getRenderDistance() {
        return renderDistance;
    }
    
    /**
     * Get the model bakery subsystem.
     */
    public ModelBakerySubsystem getModelService() {
        return modelService;
    }
    
    /**
     * Get the node manager.
     */
    public AsyncNodeManager getNodeManager() {
        return nodeManager;
    }
    
    @Override
    public void close() {
        Logger.info("Closing VoxyRenderSystem...");
        
        isInitialized = false;
        
        // Stop services
        if (renderGen != null) renderGen.stop();
        if (nodeManager != null) nodeManager.stop();
        
        // Close subsystems - wrap in try-catch since AutoCloseable.close() throws Exception
        try {
            if (pipeline != null) pipeline.close();
        } catch (Exception e) { Logger.error("Error closing pipeline", e); }
        
        try {
            if (sectionRenderer != null) sectionRenderer.close();
        } catch (Exception e) { Logger.error("Error closing sectionRenderer", e); }
        
        try {
            if (traversal != null) traversal.close();
        } catch (Exception e) { Logger.error("Error closing traversal", e); }
        
        try {
            if (nodeManager != null) nodeManager.close();
        } catch (Exception e) { Logger.error("Error closing nodeManager", e); }
        
        try {
            if (geometryData != null) geometryData.close();
        } catch (Exception e) { Logger.error("Error closing geometryData", e); }
        
        try {
            if (modelService != null) modelService.close();
        } catch (Exception e) { Logger.error("Error closing modelService", e); }
        
        if (sceneUniformBuffer != null) sceneUniformBuffer.close();
        
        worldEngine.releaseRef();
        
        Logger.info("VoxyRenderSystem closed");
    }
}
