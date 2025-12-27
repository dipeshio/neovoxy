package me.cortex.neovoxy.client.core;

import me.cortex.neovoxy.client.core.rendering.hierachical.AsyncNodeManager;
import me.cortex.neovoxy.client.core.rendering.hierachical.HierarchicalOcclusionTraverser;
import me.cortex.neovoxy.client.core.rendering.hierachical.NodeCleaner;
import me.cortex.neovoxy.common.Logger;

import java.util.function.BooleanSupplier;

/**
 * Factory for creating render pipelines based on mod environment.
 */
public final class RenderPipelineFactory {
    
    private RenderPipelineFactory() {}
    
    /**
     * Create the appropriate render pipeline.
     * 
     * @param nodeManager The node manager
     * @param nodeCleaner The node cleaner  
     * @param traversal The occlusion traverser
     * @param frexSupplier Supplier indicating if FREX is active
     * @return The created pipeline
     */
    public static AbstractRenderPipeline createPipeline(
            AsyncNodeManager nodeManager,
            NodeCleaner nodeCleaner,
            HierarchicalOcclusionTraverser traversal,
            BooleanSupplier frexSupplier) {
        
        // TODO: Detect Oculus/Iris for shader pipeline
        // For now, always use normal pipeline
        
        Logger.info("Creating NormalRenderPipeline");
        return new NormalRenderPipeline(nodeManager, nodeCleaner, traversal);
    }
    
    /**
     * Check if Oculus shaders are active.
     */
    public static boolean isOculusActive() {
        // TODO: Check for Oculus mod and active shader pack
        return false;
    }
}
