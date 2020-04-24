package squeek.appleskin.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
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
	private static ItemStack cachedItemStack;

	@Inject(at = @At("HEAD"), method = "renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemStack;II)V")
	private void renderTooltipStart(MatrixStack matrixStack, ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		cachedItemStack = itemStack;
	}

	@Inject(at = @At("TAIL"), method = "renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/item/ItemStack;II)V")
	private void renderTooltipEnd(MatrixStack matrixStack, ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		cachedItemStack = null;
	}

	@Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/item/ItemRenderer;zOffset:F", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD, method = "renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/List;II)V")
	private void renderTooltipCB(MatrixStack matrixStack, List tooltip, int mouseX, int mouseY, CallbackInfo info, int w, int x, int y, int w2, int h)
	{
		TooltipOverlayHandler.onRenderTooltip(matrixStack, cachedItemStack, x, y, w, h);
	}
}
