package squeek.appleskin.mixin;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;

@Mixin(mezz.jei.fabric.platform.RenderHelper.class)
public class JEIRenderHelperMixin
{
	// Similar janky solution as the one detailed in REITooltipPlugin, but
	// even jankier due to the use of a mixin. Right before JEI does its
	// tooltip list -> TooltipComponent mapping, we swoop in and swap out
	// our OrderedText-smuggling-a-TooltipData for the TooltipData it is
	// smuggling. This avoids JEI converting it into an empty string, and
	// gets our TooltipData into the list in the same spot as our OrderedText.

	@Inject(at = @At("HEAD"), method = "renderTooltip", require = 0)
	private void renderFoodPre(GuiGraphicsExtractor guiGraphics, List<Either<FormattedText, TooltipComponent>> elements, int x, int y, Font font, ItemStack stack, CallbackInfo info)
	{
		for (int i = 0; i < elements.size(); i++)
		{
			var element = elements.get(i);
			var maybeLeft = element.left();
			if (maybeLeft.isEmpty()) continue;
			var left = maybeLeft.get();
			if (left instanceof TooltipOverlayHandler.FoodOverlayTextComponent)
			{
				var tooltipData = ((TooltipOverlayHandler.FoodOverlayTextComponent) left).foodOverlay;
				elements.set(i, Either.right(tooltipData));
			}
		}
	}
}
