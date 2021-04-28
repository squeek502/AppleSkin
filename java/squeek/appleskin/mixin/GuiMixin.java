package squeek.appleskin.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;

@Mixin(Screen.class)
public class GuiMixin
{
	@Inject(at = @At("RETURN"), method = "getTooltipFromItem(Lnet/minecraft/item/ItemStack;)Ljava/util/List;")
	private void getTooltipFromItem(ItemStack itemStack, CallbackInfoReturnable<List> info)
	{
		List tooltip = info.getReturnValue();
		TooltipOverlayHandler.onItemTooltip(itemStack, tooltip);
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD, method = "renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/List;II)V")
	private void renderTooltip(MatrixStack matrixStack, List tooltip, int mouseX, int mouseY, CallbackInfo info, int w, int x, int y, int w2, int h)
	{
		TooltipOverlayHandler.onRenderTooltip(matrixStack, tooltip, x, y, w, h);
	}
}
