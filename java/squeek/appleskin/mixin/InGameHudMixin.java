package squeek.appleskin.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.client.HudOverlayHandler;

@Mixin(InGameHud.class)
public class InGameHudMixin
{
	@Inject(at = @At(value = "CONSTANT", args = "stringValue=food", shift = At.Shift.BY, by = 2), method = "method_1760()V")
	private void renderFoodPre(CallbackInfo info)
	{
		HudOverlayHandler.onPreRender();
	}

	@Inject(slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=food")), at = @At(value = "IINC", args = "intValue=-10", ordinal = 0), method = "method_1760()V")
	private void renderFoodPost(CallbackInfo info)
	{
		HudOverlayHandler.onRender();
	}
}
