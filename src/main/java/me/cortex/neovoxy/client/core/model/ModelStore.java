package me.cortex.neovoxy.client.core.model;

import me.cortex.neovoxy.client.core.gl.GlBuffer;
import me.cortex.neovoxy.common.Logger;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.lwjgl.opengl.GL43.*;
import static org.lwjgl.opengl.GL44.*;

/**
 * Stores block model data for LOD rendering.
 * 
 * <p>Each block model contains:
 * <ul>
 *   <li>Face data for each of 6 directions (UV, alpha cutout, tint)</li>
 *   <li>Color tint index or direct color</li>
 *   <li>Flags for translucency, shading, mipmaps</li>
 * </ul>
 * 
 * <p>Model data is stored in a GPU buffer and indexed by model ID in shaders.
 */
public class ModelStore implements AutoCloseable {
    
    // Size of each model entry in bytes (matches BlockModel struct in shaders)
    private static final int MODEL_SIZE = 32; // 8 uints
    
    private final GlBuffer modelBuffer;
    private final GlBuffer colourBuffer;
    
    private final ConcurrentHashMap<BlockState, Integer> stateToId = new ConcurrentHashMap<>();
    private final AtomicInteger nextId = new AtomicInteger(1); // 0 = air/empty
    
    private final int maxModels;
    
    public ModelStore(int maxModels) {
        this.maxModels = maxModels;
        
        // Model data buffer
        this.modelBuffer = new GlBuffer((long) maxModels * MODEL_SIZE, GL_DYNAMIC_STORAGE_BIT);
        
        // Color lookup buffer (for biome-dependent colors)
        // 256 biomes * 4 bytes per color
        this.colourBuffer = new GlBuffer(256 * 4 * maxModels, GL_DYNAMIC_STORAGE_BIT);
        
        Logger.info("ModelStore created for {} models", maxModels);
    }
    
    /**
     * Get or create model ID for a block state.
     */
    public int getModelId(BlockState state) {
        if (state.isAir()) {
            return 0;
        }
        
        return stateToId.computeIfAbsent(state, s -> {
            int id = nextId.getAndIncrement();
            if (id >= maxModels) {
                Logger.error("ModelStore capacity exceeded!");
                return 0;
            }
            
            // TODO: Actually bake the model and upload
            // For now, just assign the ID
            
            return id;
        });
    }
    
    /**
     * Get the model buffer ID for shader binding.
     */
    public int getModelBufferId() {
        return modelBuffer.id();
    }
    
    /**
     * Get the colour buffer ID for shader binding.
     */
    public int getColourBufferId() {
        return colourBuffer.id();
    }
    
    /**
     * Bind model and colour buffers to shader storage.
     */
    public void bind(int modelBinding, int colourBinding) {
        modelBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, modelBinding);
        colourBuffer.bindBase(GL_SHADER_STORAGE_BUFFER, colourBinding);
    }
    
    /**
     * Get current model count.
     */
    public int getModelCount() {
        return nextId.get();
    }
    
    @Override
    public void close() {
        modelBuffer.close();
        colourBuffer.close();
        Logger.info("ModelStore closed");
    }
}
