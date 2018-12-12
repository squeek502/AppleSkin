package squeek.appleskin.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;

@Mixin(Gui.class)
public class GuiMixin
{
	private static ItemStack cachedItemStack;

	@Inject(at = @At("HEAD"), method = "drawStackTooltip(Lnet/minecraft/item/ItemStack;II)V")
	private void drawStackTooltipStart(ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		cachedItemStack = itemStack;
	}

	@Inject(at = @At("TAIL"), method = "drawStackTooltip(Lnet/minecraft/item/ItemStack;II)V")
	private void drawStackTooltipEnd(ItemStack itemStack, int x, int y, CallbackInfo info)
	{
		cachedItemStack = null;
	}

	@Inject(at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/Gui;zOffset:F", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD, method = "drawTooltip(Ljava/util/List;II)V")
	private void drawTooltipCB(List tooltip, int mouseX, int mouseY, CallbackInfo info, int w, int x, int y, int w2, int h)
	{
		TooltipOverlayHandler.onRenderTooltip(cachedItemStack, x, y, w, h);
	}
}
