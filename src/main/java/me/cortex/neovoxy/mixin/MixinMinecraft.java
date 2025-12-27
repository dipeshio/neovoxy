package me.cortex.neovoxy.mixin;

import me.cortex.neovoxy.commonImpl.VoxyCommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin for Minecraft client tick and lifecycle hooks.
 */
@Mixin(Minecraft.class)
public class MixinMinecraft {
    
    /**
     * Tick Voxy instance each client tick.
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void neovoxy$onTick(CallbackInfo ci) {
        var instance = VoxyCommon.getInstance();
        if (instance != null) {
            instance.tick();
        }
    }
    
    /**
     * Clean up when disconnecting from world.
     * 1.21.1 signature: disconnect(Screen screen, boolean transferring)
     */
    @Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;Z)V", at = @At("HEAD"))
    private void neovoxy$onDisconnect(Screen screen, boolean transferring, CallbackInfo ci) {
        VoxyCommon.closeInstance();
    }
}
