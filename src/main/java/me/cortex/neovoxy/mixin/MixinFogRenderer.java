package me.cortex.neovoxy.mixin;

import me.cortex.neovoxy.client.core.gl.GlBuffer;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to extract fog parameters for LOD rendering.
 */
@Mixin(FogRenderer.class)
public class MixinFogRenderer {
    
    @Unique
    private static float neovoxy$fogStart;
    @Unique
    private static float neovoxy$fogEnd;
    @Unique
    private static float neovoxy$fogRed;
    @Unique
    private static float neovoxy$fogGreen;
    @Unique
    private static float neovoxy$fogBlue;
    
    /**
     * Capture fog color.
     */
    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void neovoxy$captureFogColor(CallbackInfo ci) {
        // Get fog color from render system
        float[] color = com.mojang.blaze3d.systems.RenderSystem.getShaderFogColor();
        if (color != null && color.length >= 3) {
            neovoxy$fogRed = color[0];
            neovoxy$fogGreen = color[1];
            neovoxy$fogBlue = color[2];
        }
    }
    
    /**
     * Get captured fog color for LOD rendering.
     */
    public static float[] getFogColor() {
        return new float[] { neovoxy$fogRed, neovoxy$fogGreen, neovoxy$fogBlue };
    }
    
    /**
     * Get fog start distance.
     */
    public static float getFogStart() {
        return neovoxy$fogStart;
    }
    
    /**
     * Get fog end distance.
     */
    public static float getFogEnd() {
        return neovoxy$fogEnd;
    }
}
