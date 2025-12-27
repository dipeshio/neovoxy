package me.cortex.neovoxy.client.core;

import me.cortex.neovoxy.client.core.model.ModelBakerySubsystem;
import me.cortex.neovoxy.client.core.rendering.hierachical.AsyncNodeManager;
import me.cortex.neovoxy.client.core.rendering.hierachical.HierarchicalOcclusionTraverser;
import me.cortex.neovoxy.client.core.rendering.hierachical.NodeCleaner;
import me.cortex.neovoxy.client.core.rendering.section.backend.AbstractSectionRenderer;

/**
 * Abstract base class for render pipelines.
 * 
 * <p>Pipelines handle the actual rendering of LOD sections,
 * with different implementations for vanilla and shader-modded rendering.
 */
public abstract class AbstractRenderPipeline implements AutoCloseable {
    
    protected final AsyncNodeManager nodeManager;
    protected final NodeCleaner nodeCleaner;
    protected final HierarchicalOcclusionTraverser traversal;
    
    protected AbstractSectionRenderer sectionRenderer;
    
    protected AbstractRenderPipeline(AsyncNodeManager nodeManager, 
                                     NodeCleaner nodeCleaner,
                                     HierarchicalOcclusionTraverser traversal) {
        this.nodeManager = nodeManager;
        this.nodeCleaner = nodeCleaner;
        this.traversal = traversal;
    }
    
    /**
     * Set the section renderer.
     */
    public void setSectionRenderer(AbstractSectionRenderer renderer) {
        this.sectionRenderer = renderer;
    }
    
    /**
     * Set up extra model bakery data required by this pipeline.
     */
    public abstract void setupExtraModelBakeryData(ModelBakerySubsystem modelBakery);
    
    /**
     * Get render scaling factor for this pipeline (for shader mods).
     * @return [scaleX, scaleY] or null for 1:1 scaling
     */
    public float[] getRenderScalingFactor() {
        return null;
    }
    
    /**
     * Render opaque geometry.
     */
    public abstract void renderOpaque();
    
    /**
     * Render translucent geometry.
     */
    public abstract void renderTranslucent();
    
    /**
     * Called after rendering is complete.
     */
    public abstract void postRender();
}
