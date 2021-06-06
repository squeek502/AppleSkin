package squeek.appleskin.mixin;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipData;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;

@Mixin(Screen.class)
public class ScreenMixin
{
	// The idea here is to manually swap out the auto-created OrderedTextTooltipComponent
	// for our more specific one at each place TooltipComponent::of is called
	//
	// Ideally, we'd just inject TooltipComponent::of and be done with it,
	// but Mixin doesn't support injecting static interface methods:
	// https://github.com/SpongePowered/Mixin/issues/318
	//
	// Note that we are assuming a 1:1 relationship between Text and TooltipComponent,
	// and that each Text and corresponding TooltipComponent use the same index
	// in their lists. If either of those assumptions are untrue, we'll get some
	// funky results.
	//
	// Note also that Screen.renderOrderedTooltip is not being injected,
	// even though that's another place that calls TooltipComponent::of.
	// As far as I can tell, it's not possible to Mixin Screen.renderOrderedTooltip
	// in a way that would allow us to manipulate the TooltipComponent list
	// (since it only lives on the stack). This shouldn't be a problem, though,
	// since that method is not intended for ItemStack tooltips--it's only used
	// internally for text-only tooltips and never for ItemStack tooltips.

	@Inject(
		// For some reason the local was failing to capture if INVOKE_ASSIGN was used
		// with target=Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;
		// so instead we get the start of the ifPresent and shift backwards
		// Unfortunately this is flakier than it probably should be.
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V",
			ordinal = 0,
			shift = At.Shift.BY,
			by = -3
		),
		method = "renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Ljava/util/List;Ljava/util/Optional;II)V",
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onRenderTooltip(MatrixStack matrices, List<Text> textList, Optional<TooltipData> data, int x, int y, CallbackInfo info, List<TooltipComponent> componentList)
	{
		for (final ListIterator<Text> it = textList.listIterator(); it.hasNext(); )
		{
			final Text text = it.next();
			if (text instanceof TooltipOverlayHandler.FoodOverlayTextComponent)
			{
				componentList.set(it.previousIndex(), ((TooltipOverlayHandler.FoodOverlayTextComponent) text).foodOverlay);
			}
		}
	}
}
