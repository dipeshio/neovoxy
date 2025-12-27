package me.cortex.neovoxy.mixin;

import me.cortex.neovoxy.NeoVoxyClient;
import me.cortex.neovoxy.client.VoxyClientInstance;
import me.cortex.neovoxy.client.core.IGetVoxyRenderSystem;
import me.cortex.neovoxy.client.core.VoxyRenderSystem;
import me.cortex.neovoxy.common.Logger;
import me.cortex.neovoxy.commonImpl.VoxyCommon;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to inject VoxyRenderSystem lifecycle into LevelRenderer.
 */
@Mixin(LevelRenderer.class)
public class MixinLevelRenderer implements IGetVoxyRenderSystem {
    
    @Unique
    private VoxyRenderSystem neovoxy$renderSystem;
    
    @Override
    public VoxyRenderSystem getVoxyRenderSystem() {
        return neovoxy$renderSystem;
    }
    
    @Override
    public void setVoxyRenderSystem(VoxyRenderSystem system) {
        this.neovoxy$renderSystem = system;
    }
    
    /**
     * Clean up render system when level renderer is closed.
     */
    @Inject(method = "close", at = @At("HEAD"))
    private void neovoxy$onClose(CallbackInfo ci) {
        if (neovoxy$renderSystem != null) {
            Logger.info("Cleaning up VoxyRenderSystem from LevelRenderer");
            try {
                neovoxy$renderSystem.close();
            } catch (Exception e) {
                Logger.error("Error closing VoxyRenderSystem", e);
            }
            neovoxy$renderSystem = null;
        }
    }
}
