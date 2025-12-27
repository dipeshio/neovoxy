package me.cortex.neovoxy.client.core.gl;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL42.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * Wrapper for OpenGL texture objects with DSA support.
 */
public class GlTexture implements AutoCloseable {
    
    private final int id;
    private final int target;
    private final int width;
    private final int height;
    private final int depth;
    private final int internalFormat;
    private boolean isClosed = false;
    
    /**
     * Create a 2D texture.
     */
    public static GlTexture create2D(int width, int height, int internalFormat, int levels) {
        int id = glCreateTextures(GL_TEXTURE_2D);
        glTextureStorage2D(id, levels, internalFormat, width, height);
        return new GlTexture(id, GL_TEXTURE_2D, width, height, 1, internalFormat);
    }
    
    /**
     * Create a 2D array texture.
     */
    public static GlTexture create2DArray(int width, int height, int layers, int internalFormat, int levels) {
        int id = glCreateTextures(GL_TEXTURE_2D_ARRAY);
        glTextureStorage3D(id, levels, internalFormat, width, height, layers);
        return new GlTexture(id, GL_TEXTURE_2D_ARRAY, width, height, layers, internalFormat);
    }
    
    /**
     * Create a 3D texture.
     */
    public static GlTexture create3D(int width, int height, int depth, int internalFormat, int levels) {
        int id = glCreateTextures(GL_TEXTURE_3D);
        glTextureStorage3D(id, levels, internalFormat, width, height, depth);
        return new GlTexture(id, GL_TEXTURE_3D, width, height, depth, internalFormat);
    }
    
    private GlTexture(int id, int target, int width, int height, int depth, int internalFormat) {
        this.id = id;
        this.target = target;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.internalFormat = internalFormat;
    }
    
    public int id() {
        return id;
    }
    
    public int target() {
        return target;
    }
    
    public int width() {
        return width;
    }
    
    public int height() {
        return height;
    }
    
    public int depth() {
        return depth;
    }
    
    /**
     * Bind texture to a texture unit.
     */
    public void bind(int unit) {
        glBindTextureUnit(unit, id);
    }
    
    /**
     * Bind texture for image load/store operations.
     */
    public void bindImage(int unit, int level, boolean layered, int layer, int access, int format) {
        glBindImageTexture(unit, id, level, layered, layer, access, format);
    }
    
    /**
     * Set texture parameter.
     */
    public void parameter(int pname, int param) {
        glTextureParameteri(id, pname, param);
    }
    
    /**
     * Set texture parameter (float).
     */
    public void parameter(int pname, float param) {
        glTextureParameterf(id, pname, param);
    }
    
    /**
     * Configure for typical LOD texture sampling.
     */
    public void configureForLOD() {
        parameter(GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        parameter(GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        parameter(GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        parameter(GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
    }
    
    /**
     * Configure for nearest-neighbor sampling.
     */
    public void configureNearest() {
        parameter(GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        parameter(GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }
    
    /**
     * Generate mipmaps.
     */
    public void generateMipmaps() {
        glGenerateTextureMipmap(id);
    }
    
    @Override
    public void close() {
        if (!isClosed) {
            glDeleteTextures(id);
            isClosed = true;
        }
    }
}
