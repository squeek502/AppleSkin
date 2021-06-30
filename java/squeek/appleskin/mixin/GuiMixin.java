package squeek.appleskin.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;

@Mixin(Screen.class)
public class GuiMixin
{
	// captured before the tooltip is rendered and stored here temporarily
	private static int x;
	private static int y;
	private static int w;
	private static int h;

	// capture x, y, etc at matrixStack.push()
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD, method = "renderOrderedTooltip(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/List;II)V")
	private void renderTooltipCaptureLocals(MatrixStack matrixStack, List tooltip, int mouseX, int mouseY, CallbackInfo info, int w, int x, int y, int w2, int h)
	{
		GuiMixin.x = x;
		GuiMixin.y = y;
		GuiMixin.w = w;
		GuiMixin.h = h;
	}

	// do the actual rendering at matrixStack.pop()
	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;pop()V", ordinal = 0), method = "renderOrderedTooltip(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/List;II)V")
	private void renderTooltip(MatrixStack matrixStack, List tooltip, int mouseX, int mouseY, CallbackInfo info)
	{
		TooltipOverlayHandler.onRenderTooltip(matrixStack, tooltip, x, y, w, h);
	}
}
