package me.cortex.neovoxy.client.core.rendering.hierachical;

import me.cortex.neovoxy.client.core.gl.GlBuffer;
import me.cortex.neovoxy.client.core.gl.GlShader;
import me.cortex.neovoxy.client.core.rendering.Viewport;
import me.cortex.neovoxy.common.Logger;

import static org.lwjgl.opengl.GL43.*;

/**
 * GPU-driven hierarchical occlusion traversal using compute shaders.
 * 
 * <p>
 * Traverses the LOD node tree on the GPU, performing:
 * <ul>
 * <li>Frustum culling</li>
 * <li>Hierarchical Z-buffer (HiZ) occlusion</li>
 * <li>Screen-space size-based LOD selection</li>
 * </ul>
 * 
 * <p>
 * Outputs a list of visible sections to render.
 */
public class HierarchicalOcclusionTraverser implements AutoCloseable {

    private static final int LOCAL_SIZE = 128;
    private static final int MAX_ITERATIONS = 10;

    private final AsyncNodeManager nodeManager;
    private final NodeCleaner nodeCleaner;
    private final RenderGenerationService renderGen;
    private Viewport viewport;

    // Compute shaders
    private GlShader traversalShader;

    // Queue buffers for traversal
    private GlBuffer requestQueue;
    private GlBuffer renderQueue;

    private boolean isInitialized = false;

    public HierarchicalOcclusionTraverser(AsyncNodeManager nodeManager,
            NodeCleaner nodeCleaner,
            RenderGenerationService renderGen) {
        this.nodeManager = nodeManager;
        this.nodeCleaner = nodeCleaner;
        this.renderGen = renderGen;
    }

    public void setViewport(Viewport viewport) {
        this.viewport = viewport;
    }

    public Viewport getViewport() {
        return viewport;
    }

    /**
     * Initialize traversal shaders and buffers.
     */
    public void initialize() {
        if (isInitialized)
            return;

        try {
            // Load traversal compute shader
            String traversalSource = GlShader.Builder.loadSource("lod/hierarchical/traversal_dev.comp");

            traversalShader = new GlShader.Builder()
                    .compute(traversalSource)
                    .define("LOCAL_SIZE_BITS", 7) // 128 threads
                    .define("MAX_ITERATIONS", MAX_ITERATIONS)
                    // Binding point defines required by shaders
                    .define("SCENE_UNIFORM_BINDING", 0)
                    .define("NODE_QUEUE_INDEX_BINDING", 0) // uniform location
                    .define("NODE_QUEUE_META_BINDING", 1)
                    .define("NODE_QUEUE_SOURCE_BINDING", 2)
                    .define("NODE_QUEUE_SINK_BINDING", 3)
                    .define("NODE_DATA_BINDING", 4)
                    .define("HIZ_BINDING", 5)
                    .define("REQUEST_QUEUE_BINDING", 6)
                    .define("RENDER_QUEUE_BINDING", 7)
                    .define("RENDER_TRACKER_BINDING", 8)
                    .define("MAX_REQUEST_QUEUE_SIZE", "65536") // Use string value
                    .build();

            // Allocate queue buffers
            int maxRequests = 1 << 16;
            int maxRender = 1 << 20;

            requestQueue = new GlBuffer((long) maxRequests * 8 + 8,
                    org.lwjgl.opengl.GL44.GL_DYNAMIC_STORAGE_BIT);
            renderQueue = new GlBuffer((long) maxRender * 4 + 4,
                    org.lwjgl.opengl.GL44.GL_DYNAMIC_STORAGE_BIT);

            isInitialized = true;
            Logger.info("HierarchicalOcclusionTraverser initialized");

        } catch (Exception e) {
            Logger.error("Failed to initialize traversal shaders", e);
        }
    }

    /**
     * Run hierarchical traversal.
     * 
     * @param frameId        Current frame number
     * @param viewportWidth  Viewport width in pixels
     * @param viewportHeight Viewport height in pixels
     */
    public void traverse(int frameId, int viewportWidth, int viewportHeight) {
        if (!isInitialized) {
            initialize();
            if (!isInitialized)
                return;
        }

        // TODO: Set up uniform buffer with frustum, camera, etc.

        // Clear render queue counter
        renderQueue.upload(0, new int[] { 0 });

        // Bind buffers
        requestQueue.bindBase(GL_SHADER_STORAGE_BUFFER, 0);
        renderQueue.bindBase(GL_SHADER_STORAGE_BUFFER, 1);
        nodeManager.bindForTraversal(2, 3, 4);

        // Use traversal shader
        traversalShader.use();

        // Dispatch compute
        int nodeCount = nodeManager.getNodeCount();
        int workGroups = (nodeCount + LOCAL_SIZE - 1) / LOCAL_SIZE;

        glDispatchCompute(workGroups, 1, 1);

        // Memory barrier for subsequent draw commands
        glMemoryBarrier(GL_SHADER_STORAGE_BARRIER_BIT | GL_COMMAND_BARRIER_BIT);

        GlShader.unbind();
    }

    /**
     * Get the render queue buffer for indirect rendering.
     */
    public GlBuffer getRenderQueue() {
        return renderQueue;
    }

    @Override
    public void close() {
        if (traversalShader != null) {
            traversalShader.close();
        }
        if (requestQueue != null) {
            requestQueue.close();
        }
        if (renderQueue != null) {
            renderQueue.close();
        }
        isInitialized = false;
        Logger.info("HierarchicalOcclusionTraverser closed");
    }
}
