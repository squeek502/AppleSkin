package squeek.appleskin.mixin;

import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.client.HUDOverlayHandler;

@Mixin(InGameHud.class)
public class InGameHudMixin
{
	@Inject(at = @At(value = "CONSTANT", args = "stringValue=food", shift = At.Shift.BY, by = 2), method = "renderStatusBars(Lnet/minecraft/client/util/math/MatrixStack;)V")
	private void renderFoodPre(MatrixStack stack, CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onPreRender(stack);
	}

	@Inject(slice = @Slice(from = @At(value = "CONSTANT", args = "stringValue=food")), at = @At(value = "APPLESKIN_IINC", args = "intValue=-10", ordinal = 0), method = "renderStatusBars(Lnet/minecraft/client/util/math/MatrixStack;)V")
	private void renderFoodPost(MatrixStack stack, CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onRender(stack);
	}
}
