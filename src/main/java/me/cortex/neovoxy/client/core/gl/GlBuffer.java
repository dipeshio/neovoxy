package me.cortex.neovoxy.client.core.gl;

import me.cortex.neovoxy.common.Logger;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL44.*;
import static org.lwjgl.opengl.GL45.*;

/**
 * Wrapper for OpenGL buffer objects with DSA (Direct State Access).
 * Provides type-safe buffer management with automatic cleanup.
 */
public class GlBuffer implements AutoCloseable {
    
    private final int id;
    private final long size;
    private final int flags;
    private boolean isClosed = false;
    
    /**
     * Create a buffer with immutable storage.
     * 
     * @param size Size in bytes
     * @param flags GL storage flags (GL_DYNAMIC_STORAGE_BIT, GL_MAP_READ_BIT, etc.)
     */
    public GlBuffer(long size, int flags) {
        this.id = glCreateBuffers();
        this.size = size;
        this.flags = flags;
        
        glNamedBufferStorage(id, size, flags);
    }
    
    /**
     * Create a buffer with initial data.
     */
    public GlBuffer(ByteBuffer data, int flags) {
        this.id = glCreateBuffers();
        this.size = data.remaining();
        this.flags = flags;
        
        glNamedBufferStorage(id, data, flags);
    }
    
    /**
     * Create a buffer with initial int array data.
     */
    public GlBuffer(int[] data, int flags) {
        this.id = glCreateBuffers();
        this.size = (long) data.length * Integer.BYTES;
        this.flags = flags;
        
        glNamedBufferStorage(id, data, flags);
    }
    
    /**
     * Create a buffer with initial long array data.
     */
    public GlBuffer(long[] data, int flags) {
        this.id = glCreateBuffers();
        this.size = (long) data.length * Long.BYTES;
        this.flags = flags;
        
        glNamedBufferStorage(id, data, flags);
    }
    
    public int id() {
        return id;
    }
    
    public long size() {
        return size;
    }
    
    /**
     * Upload data to a subregion of the buffer.
     */
    public void upload(long offset, ByteBuffer data) {
        glNamedBufferSubData(id, offset, data);
    }
    
    /**
     * Upload int array to a subregion.
     */
    public void upload(long offset, int[] data) {
        glNamedBufferSubData(id, offset, data);
    }
    
    /**
     * Upload long array to a subregion.
     */
    public void upload(long offset, long[] data) {
        glNamedBufferSubData(id, offset, data);
    }
    
    /**
     * Bind buffer to indexed binding point (for SSBO/UBO).
     */
    public void bindBase(int target, int index) {
        glBindBufferBase(target, index, id);
    }
    
    /**
     * Bind a range of the buffer to indexed binding point.
     */
    public void bindRange(int target, int index, long offset, long size) {
        glBindBufferRange(target, index, id, offset, size);
    }
    
    /**
     * Copy data from another buffer.
     */
    public void copyFrom(GlBuffer source, long srcOffset, long dstOffset, long size) {
        glCopyNamedBufferSubData(source.id, this.id, srcOffset, dstOffset, size);
    }
    
    /**
     * Clear buffer to zero.
     */
    public void clear() {
        glClearNamedBufferData(id, GL_R32UI, GL_RED_INTEGER, GL_UNSIGNED_INT, new int[]{0});
    }
    
    @Override
    public void close() {
        if (!isClosed) {
            glDeleteBuffers(id);
            isClosed = true;
        }
    }
    
    /**
     * Builder for creating buffers with common configurations.
     */
    public static class Builder {
        private long size;
        private int flags = 0;
        private ByteBuffer initialData;
        
        public Builder size(long size) {
            this.size = size;
            return this;
        }
        
        public Builder dynamicStorage() {
            this.flags |= GL_DYNAMIC_STORAGE_BIT;
            return this;
        }
        
        public Builder mapRead() {
            this.flags |= GL_MAP_READ_BIT;
            return this;
        }
        
        public Builder mapWrite() {
            this.flags |= GL_MAP_WRITE_BIT;
            return this;
        }
        
        public Builder mapPersistent() {
            this.flags |= GL_MAP_PERSISTENT_BIT;
            return this;
        }
        
        public Builder mapCoherent() {
            this.flags |= GL_MAP_COHERENT_BIT;
            return this;
        }
        
        public Builder clientStorage() {
            this.flags |= GL_CLIENT_STORAGE_BIT;
            return this;
        }
        
        public Builder initialData(ByteBuffer data) {
            this.initialData = data;
            return this;
        }
        
        public GlBuffer build() {
            if (initialData != null) {
                return new GlBuffer(initialData, flags);
            }
            return new GlBuffer(size, flags);
        }
    }
}
