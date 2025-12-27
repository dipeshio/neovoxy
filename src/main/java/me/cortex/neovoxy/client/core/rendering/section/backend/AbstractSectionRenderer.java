package me.cortex.neovoxy.client.core.rendering.section.backend;

import me.cortex.neovoxy.client.core.AbstractRenderPipeline;
import me.cortex.neovoxy.client.core.model.ModelStore;
import me.cortex.neovoxy.client.core.rendering.section.geometry.IGeometryData;
import me.cortex.neovoxy.client.core.rendering.Viewport;

/**
 * Abstract base for section renderers.
 * 
 * <p>Section renderers handle the actual draw calls for LOD sections,
 * with different backends possible (MDIC, meshlets, etc.).
 */
public abstract class AbstractSectionRenderer implements AutoCloseable {
    
    protected final AbstractRenderPipeline pipeline;
    protected final ModelStore modelStore;
    protected final IGeometryData geometryData;
    
    protected AbstractSectionRenderer(AbstractRenderPipeline pipeline, 
                                       ModelStore modelStore,
                                       IGeometryData geometryData) {
        this.pipeline = pipeline;
        this.modelStore = modelStore;
        this.geometryData = geometryData;
    }
    
    /**
     * Create a viewport for this renderer.
     */
    public abstract Viewport createViewport();
    
    /**
     * Render opaque sections.
     */
    public abstract void renderOpaque(Viewport viewport);
    
    /**
     * Render translucent sections.
     */
    public abstract void renderTranslucent(Viewport viewport);
    
    /**
     * Factory interface for creating section renderers.
     */
    public interface Factory<R extends AbstractSectionRenderer, G extends IGeometryData> {
        R create(AbstractRenderPipeline pipeline, ModelStore modelStore, G geometryData);
        Class<R> clz();
    }
}
