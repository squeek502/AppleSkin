package squeek.appleskin.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;

@SuppressWarnings("unused")
@Mixin(Screen.class)
public class GuiMixin {

	private List<Text> tooltip;

	@Inject(at = @At("RETURN"), method = "getTooltipFromItem(Lnet/minecraft/item/ItemStack;)Ljava/util/List;")
	private void getTooltipFromItem(ItemStack itemStack, CallbackInfoReturnable<List<Text>> info) {
		List<Text> tooltip = info.getReturnValue();
		TooltipOverlayHandler.onItemTooltip(itemStack, tooltip);
		this.tooltip = tooltip;
	}

	@Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", ordinal = 0), locals = LocalCapture.CAPTURE_FAILHARD, method = "renderTooltipFromComponents(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/List;II)V")
	private void renderTooltip(MatrixStack matrixStack, List<TooltipComponent> tooltip, int mouseX, int mouseY, CallbackInfo info, int w, int h, int x, int y) {
		TooltipOverlayHandler.onRenderTooltip(matrixStack, tooltip, x, y, w, h);
	}

	@ModifyVariable(at = @At(value = "STORE"), method = "renderTooltipFromComponents(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/List;II)V", ordinal = 2)
	private int modifyInt(int originalValue) {
		if (this.tooltip != null) {
			for (Text text : this.tooltip) {
				if (text instanceof TooltipOverlayHandler.FoodOverlayTooltipComponent) {
					TooltipOverlayHandler.FoodOverlay foodOverlay = ((TooltipOverlayHandler.FoodOverlayTooltipComponent)text).getFoodOverlay();
					int hungerLength = foodOverlay.getHungerBars() * 9;
					int saturationLength = foodOverlay.getSaturationBars() * 7;
					originalValue = Math.max(originalValue, Math.max(hungerLength, saturationLength));
				}
			}
		}
		return originalValue;
	}
}
