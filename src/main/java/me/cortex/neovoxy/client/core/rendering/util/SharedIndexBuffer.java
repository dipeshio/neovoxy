package me.cortex.neovoxy.client.core.rendering.util;

import me.cortex.neovoxy.common.Logger;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * Shared index buffer for quad rendering.
 * 
 * <p>Voxy renders quads as two triangles using a shared index buffer.
 * This singleton manages the GPU buffer containing the index pattern:
 * [0,1,2, 2,3,0] repeated for each quad.
 */
public final class SharedIndexBuffer {
    
    public static final SharedIndexBuffer INSTANCE = new SharedIndexBuffer();
    
    private static final int MAX_QUADS = 1 << 20; // ~1 million quads
    private static final int INDICES_PER_QUAD = 6;
    
    private int bufferId = 0;
    private boolean initialized = false;
    
    private SharedIndexBuffer() {}
    
    /**
     * Get the OpenGL buffer ID, initializing if needed.
     */
    public int id() {
        if (!initialized) {
            initialize();
        }
        return bufferId;
    }
    
    private synchronized void initialize() {
        if (initialized) return;
        
        Logger.info("Creating shared index buffer for {} quads", MAX_QUADS);
        
        // Create buffer
        bufferId = glCreateBuffers();
        
        // Generate index data: 0,1,2, 2,3,0 pattern for each quad
        int[] indices = new int[MAX_QUADS * INDICES_PER_QUAD];
        for (int quad = 0; quad < MAX_QUADS; quad++) {
            int base = quad * 4;
            int idx = quad * 6;
            indices[idx + 0] = base + 0;
            indices[idx + 1] = base + 1;
            indices[idx + 2] = base + 2;
            indices[idx + 3] = base + 2;
            indices[idx + 4] = base + 3;
            indices[idx + 5] = base + 0;
        }
        
        // Upload to GPU
        glNamedBufferStorage(bufferId, indices, 0);
        
        initialized = true;
        Logger.info("Shared index buffer created: {} bytes", indices.length * 4);
    }
    
    /**
     * Bind the index buffer for rendering.
     */
    public void bind() {
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, id());
    }
    
    /**
     * Clean up the buffer.
     */
    public void cleanup() {
        if (initialized && bufferId != 0) {
            glDeleteBuffers(bufferId);
            bufferId = 0;
            initialized = false;
        }
    }
}
