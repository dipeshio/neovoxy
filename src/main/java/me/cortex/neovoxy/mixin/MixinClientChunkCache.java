package me.cortex.neovoxy.mixin;

import net.minecraft.client.multiplayer.ClientChunkCache;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin to intercept chunk loading for LOD generation.
 */
@Mixin(ClientChunkCache.class)
public class MixinClientChunkCache {
    // TODO: Intercept chunk loading/unloading for voxelization
    // - replaceWithPacketData() - chunk received from server
    // - drop() - chunk unloaded
}
