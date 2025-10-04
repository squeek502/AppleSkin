package squeek.appleskin.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.DebugHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import squeek.appleskin.client.DebugInfoHandler;

import java.util.List;

@Mixin(DebugHud.class)
public class DebugHudMixin
{
	@Inject(at = @At("HEAD"), method = "Lnet/minecraft/client/gui/hud/DebugHud;drawText(Lnet/minecraft/client/gui/DrawContext;Ljava/util/List;Z)V")
	protected void render(DrawContext context, List<String> text, boolean left, CallbackInfo ci)
	{
		if (DebugInfoHandler.INSTANCE != null && left)
			DebugInfoHandler.INSTANCE.onTextRender(text);
	}
}
