package me.cortex.neovoxy.mixin;

import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.common.voxelization.VoxelizedSection;
import me.cortex.neovoxy.common.voxelization.WorldConversionFactory;
import me.cortex.neovoxy.common.world.other.Mapper;
import me.cortex.neovoxy.commonImpl.VoxyCommon;
import me.cortex.neovoxy.client.VoxyClientInstance;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

/**
 * Mixin to intercept chunk loading for LOD generation.
 */
@Mixin(ClientChunkCache.class)
public class MixinClientChunkCache {
    
    @Unique
    private WorldConversionFactory neovoxy$conversionFactory;
    
    /**
     * Intercept chunk arrival from server.
     */
    @Inject(method = "replaceWithPacketData", at = @At("RETURN"))
    private void neovoxy$onChunkLoaded(int x, int z, CallbackInfoReturnable<LevelChunk> cir) {
        LevelChunk chunk = cir.getReturnValue();
        if (chunk == null) return;
        
        var instance = VoxyCommon.getInstance();
        if (instance == null || !(instance instanceof VoxyClientInstance clientInstance)) return;
        
        var worldEngine = clientInstance.getWorldEngine();
        if (worldEngine == null) return;
        
        Mapper mapper = worldEngine.getMapper();
        if (mapper == null) return;
        
        // Lazily initialize conversion factory
        if (neovoxy$conversionFactory == null) {
            neovoxy$conversionFactory = new WorldConversionFactory(mapper);
        }
        
        // Convert chunk sections to LOD data
        int minSection = chunk.getLevel().getMinSection();
        int maxSection = chunk.getLevel().getMaxSection();
        
        for (int sectionY = minSection; sectionY < maxSection; sectionY++) {
            try {
                VoxelizedSection voxelized = neovoxy$conversionFactory.convertSection(chunk, sectionY);
                if (voxelized != null) {
                    // Notify world engine of new section data
                    long packedPos = VoxelizedSection.packPosition(x, sectionY, z);
                    worldEngine.notifySectionDirty(packedPos);
                }
            } catch (Exception e) {
                Logger.error("Failed to voxelize section at ({}, {}, {})", x, sectionY, z, e);
            }
        }
    }
    
    /**
     * Intercept chunk unloading.
     */
    @Inject(method = "drop", at = @At("HEAD"))
    private void neovoxy$onChunkUnloaded(int x, int z, CallbackInfo ci) {
        // Chunks that are unloaded don't need immediate LOD regeneration
        // The LOD data persists in storage
    }
}
