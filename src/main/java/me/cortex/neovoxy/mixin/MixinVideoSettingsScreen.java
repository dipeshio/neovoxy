package me.cortex.neovoxy.mixin;

import me.cortex.neovoxy.client.ui.NeoVoxySettingsScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VideoSettingsScreen.class)
public abstract class MixinVideoSettingsScreen extends Screen {
    
    protected MixinVideoSettingsScreen(Component title) {
        super(title);
    }
    
    @Inject(method = "init", at = @At("RETURN"))
    private void neovoxy$addSettingsButton(CallbackInfo ci) {
        // Add NeoVoxy button in top-right corner
        this.addRenderableWidget(Button.builder(
            Component.literal("NeoVoxy..."),
            button -> Minecraft.getInstance().setScreen(new NeoVoxySettingsScreen(this))
        ).bounds(this.width - 105, 5, 100, 20).build());
    }
}
