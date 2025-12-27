package me.cortex.neovoxy.mixin;

import com.mojang.blaze3d.platform.Window;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Mixin for Window resize handling.
 */
@Mixin(Window.class)
public class MixinWindow {
    // TODO: Handle window resize for HiZ buffer recreation
}
