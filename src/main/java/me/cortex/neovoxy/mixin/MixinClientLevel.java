package me.cortex.neovoxy.mixin;

import me.cortex.neovoxy.NeoVoxyClient;
import me.cortex.neovoxy.client.VoxyClientInstance;
import me.cortex.neovoxy.client.core.IGetVoxyRenderSystem;
import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.commonImpl.VoxyCommon;
import me.cortex.neovoxy.commonImpl.VoxyInstance;
import me.cortex.neovoxy.commonImpl.WorldIdentifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevel {
    
    @Inject(method = "<init>", at = @At("RETURN"))
    private void neovoxy$onInit(CallbackInfo ci) {
        ClientLevel level = (ClientLevel)(Object)this;
        ResourceKey<Level> dimension = level.dimension();
        
        String worldName = level.toString();
        WorldIdentifier worldId = WorldIdentifier.singleplayer(worldName, dimension);
        
        Logger.info("ClientLevel created, notifying VoxyCommon: {}", worldId);
        
        // Create the Voxy instance for this world
        VoxyInstance instance = VoxyCommon.getOrCreateInstance(worldId);
        
        // If it's a client instance, create and attach the render system
        if (instance instanceof VoxyClientInstance clientInstance) {
            if (NeoVoxyClient.isSystemSupported()) {
                // Create the render system
                clientInstance.createRenderSystem();
                
                // Attach it to the level renderer
                var levelRenderer = Minecraft.getInstance().levelRenderer;
                if (levelRenderer instanceof IGetVoxyRenderSystem vrs) {
                    vrs.setVoxyRenderSystem(clientInstance.getRenderSystem());
                    Logger.info("VoxyRenderSystem attached to LevelRenderer");
                }
            }
        }
    }
}
