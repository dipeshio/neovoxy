package me.cortex.neovoxy.mixin;

import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.commonImpl.VoxyCommon;
import me.cortex.neovoxy.commonImpl.WorldIdentifier;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

/**
 * Mixin to track ClientLevel lifecycle for Voxy instance management.
 */
@Mixin(ClientLevel.class)
public class MixinClientLevel {
    
    /**
     * Create Voxy instance when client level is created.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void neovoxy$onInit(CallbackInfo ci) {
        ClientLevel level = (ClientLevel)(Object)this;
        ResourceKey<Level> dimension = level.dimension();
        
        // Determine world identifier
        // For singleplayer: use world name
        // For multiplayer: use server address
        String worldName = level.toString(); // Placeholder - need proper world name extraction
        WorldIdentifier worldId = WorldIdentifier.singleplayer(worldName, dimension);
        
        Logger.info("ClientLevel created, notifying VoxyCommon: {}", worldId);
        
        // This will create or update the Voxy instance for this world
        VoxyCommon.getOrCreateInstance(worldId);
    }
}
