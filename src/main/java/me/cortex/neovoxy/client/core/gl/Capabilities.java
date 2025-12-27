package me.cortex.neovoxy.client.core.gl;

import me.cortex.neovoxy.common.Logger;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.opengl.GL46.*;

/**
 * GPU capability detection for Voxy rendering requirements.
 * 
 * <p>Voxy requires OpenGL 4.6 with specific extensions:
 * <ul>
 *   <li>GL_ARB_compute_shader - For GPU-driven traversal</li>
 *   <li>GL_ARB_indirect_parameters - For MDI rendering</li>
 *   <li>GL_ARB_gpu_shader_int64 - For packed quad data</li>
 *   <li>GL_ARB_shader_ballot (optional) - For subgroup operations</li>
 * </ul>
 */
public final class Capabilities {
    
    public static Capabilities INSTANCE;
    
    // Core requirements
    public final boolean compute;
    public final boolean indirectParameters;
    public final boolean shaderInt64;
    
    // Optional enhancements
    public final boolean subgroup;
    public final boolean nvBarycentric;
    
    // Known issues
    public final boolean hasBrokenDepthSampler;
    
    // GPU info
    public final String vendor;
    public final String renderer;
    public final String version;
    public final int majorVersion;
    public final int minorVersion;
    
    private Capabilities() {
        GLCapabilities caps = GL.getCapabilities();
        
        // Get GPU info
        this.vendor = glGetString(GL_VENDOR);
        this.renderer = glGetString(GL_RENDERER);
        this.version = glGetString(GL_VERSION);
        this.majorVersion = glGetInteger(GL_MAJOR_VERSION);
        this.minorVersion = glGetInteger(GL_MINOR_VERSION);
        
        Logger.info("GPU: {} ({})", renderer, vendor);
        Logger.info("OpenGL Version: {}.{}", majorVersion, minorVersion);
        
        // Check core requirements
        this.compute = caps.GL_ARB_compute_shader || majorVersion >= 4 && minorVersion >= 3;
        this.indirectParameters = caps.GL_ARB_indirect_parameters;
        this.shaderInt64 = caps.GL_ARB_gpu_shader_int64;
        
        // Check optional features
        this.subgroup = caps.GL_ARB_shader_ballot || caps.GL_KHR_shader_subgroup;
        this.nvBarycentric = caps.GL_NV_fragment_shader_barycentric;
        
        // Detect known GPU issues
        this.hasBrokenDepthSampler = detectBrokenDepthSampler();
        
        logCapabilities();
    }
    
    private boolean detectBrokenDepthSampler() {
        // Some AMD drivers have issues with depth texture sampling
        // that causes visual artifacts in the HiZ pass
        if (vendor != null && vendor.toLowerCase().contains("amd")) {
            // TODO: Check specific driver versions
            // For now, assume newer drivers have fixed this
            return false;
        }
        return false;
    }
    
    private void logCapabilities() {
        Logger.info("NeoVoxy GPU Capabilities:");
        Logger.info("  Compute Shaders: {}", compute ? "YES" : "NO");
        Logger.info("  Indirect Parameters: {}", indirectParameters ? "YES" : "NO");
        Logger.info("  Shader Int64: {}", shaderInt64 ? "YES" : "NO");
        Logger.info("  Subgroup Operations: {}", subgroup ? "YES" : "NO");
        Logger.info("  NV Barycentric: {}", nvBarycentric ? "YES" : "NO");
        
        if (hasBrokenDepthSampler) {
            Logger.warn("  Broken Depth Sampler: DETECTED");
        }
        
        if (!compute || !indirectParameters) {
            Logger.error("GPU does not meet minimum requirements for NeoVoxy!");
        }
    }
    
    /**
     * Initialize capabilities. Must be called from render thread.
     */
    public static void init() {
        if (INSTANCE == null) {
            INSTANCE = new Capabilities();
        }
    }
    
    /**
     * Check if all core requirements are met.
     */
    public boolean meetsRequirements() {
        return compute && indirectParameters && !hasBrokenDepthSampler;
    }
}
