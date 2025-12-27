package me.cortex.neovoxy.client.core.hiz;

import me.cortex.neovoxy.client.core.gl.GlShader;
import me.cortex.neovoxy.client.core.gl.GlTexture;
import me.cortex.neovoxy.common.Logger;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * Hierarchical Z-buffer for occlusion culling.
 * 
 * <p>Generates a mipmap pyramid from the depth buffer where each level
 * contains the maximum depth of the corresponding 2x2 region. This allows
 * efficient conservative visibility testing at multiple scales.
 */
public class HiZBuffer implements AutoCloseable {
    
    private GlTexture hizTexture;
    private GlShader hizShader;
    
    private int width;
    private int height;
    private int levels;
    
    private boolean isInitialized = false;
    
    public HiZBuffer() {}
    
    /**
     * Initialize or resize the HiZ buffer.
     */
    public void initialize(int width, int height) {
        if (isInitialized && this.width == width && this.height == height) {
            return;
        }
        
        cleanup();
        
        this.width = width;
        this.height = height;
        this.levels = calculateLevels(width, height);
        
        // Create HiZ texture with mipmaps
        hizTexture = GlTexture.create2D(width, height, GL_R32F, levels);
        hizTexture.configureNearest();
        hizTexture.parameter(GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_NEAREST);
        
        // Load HiZ compute shader
        try {
            String source = GlShader.Builder.loadSource("hiz/hiz.comp");
            hizShader = new GlShader.Builder()
                .compute(source)
                .build();
        } catch (Exception e) {
            Logger.error("Failed to load HiZ shader", e);
            return;
        }
        
        isInitialized = true;
        Logger.info("HiZBuffer initialized: {}x{}, {} levels", width, height, levels);
    }
    
    private int calculateLevels(int w, int h) {
        int size = Math.max(w, h);
        return (int) Math.ceil(Math.log(size) / Math.log(2)) + 1;
    }
    
    /**
     * Generate HiZ pyramid from depth texture.
     * 
     * @param depthTexture Source depth texture ID
     */
    public void generate(int depthTexture) {
        if (!isInitialized) return;
        
        hizShader.use();
        
        // Copy depth to level 0
        glCopyImageSubData(
            depthTexture, GL_TEXTURE_2D, 0, 0, 0, 0,
            hizTexture.id(), GL_TEXTURE_2D, 0, 0, 0, 0,
            width, height, 1
        );
        
        // Generate mipmap levels
        int w = width;
        int h = height;
        
        for (int level = 1; level < levels; level++) {
            int prevW = w;
            int prevH = h;
            w = Math.max(1, w / 2);
            h = Math.max(1, h / 2);
            
            // Bind previous level as input
            hizTexture.bind(0);
            hizShader.setUniform("u_PrevLevel", level - 1);
            hizShader.setUniform("u_PrevSize", prevW, prevH);
            
            // Bind current level as output image
            hizTexture.bindImage(0, level, false, 0, GL_WRITE_ONLY, GL_R32F);
            
            // Dispatch compute
            int groupsX = (w + 7) / 8;
            int groupsY = (h + 7) / 8;
            glDispatchCompute(groupsX, groupsY, 1);
            
            // Memory barrier between levels
            glMemoryBarrier(GL_SHADER_IMAGE_ACCESS_BARRIER_BIT | GL_TEXTURE_FETCH_BARRIER_BIT);
        }
        
        GlShader.unbind();
    }
    
    /**
     * Bind HiZ texture for sampling.
     */
    public void bind(int unit) {
        if (isInitialized) {
            hizTexture.bind(unit);
        }
    }
    
    /**
     * Get the HiZ texture ID.
     */
    public int getTextureId() {
        return isInitialized ? hizTexture.id() : 0;
    }
    
    private void cleanup() {
        if (hizTexture != null) {
            hizTexture.close();
            hizTexture = null;
        }
        isInitialized = false;
    }
    
    @Override
    public void close() {
        cleanup();
        if (hizShader != null) {
            hizShader.close();
            hizShader = null;
        }
        Logger.info("HiZBuffer closed");
    }
}
