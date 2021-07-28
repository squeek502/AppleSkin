package squeek.appleskin.mixin;

import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.impl.client.gui.ScreenOverlayImpl;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;
import java.util.ListIterator;

@Mixin(value = ScreenOverlayImpl.class, remap = false)
public class REITooltipMixin
{
	// See the comment in ScreenMixin for a complete explanation.
	//
	// This should be seen as a workaround until its possible to mixin TooltipComponent::of.
	// (i.e. once https://github.com/FabricMC/Mixin/pull/51 makes it into a Fabric build)

	@Inject(
		at = @At(value = "INVOKE", target = "Lme/shedaniel/rei/impl/client/gui/ScreenOverlayImpl;renderTooltipInner", ordinal = 0),
		method = "renderTooltip(Lnet/minecraft/client/util/math/MatrixStack;Lme/shedaniel/rei/api/client/gui/widgets/Tooltip;)V",
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private void onRenderTooltip(MatrixStack matrices, Tooltip tooltip, CallbackInfo info, List<TooltipComponent> componentList)
	{
		for (final ListIterator<Tooltip.Entry> it = tooltip.entries().listIterator(); it.hasNext(); )
		{
			final Tooltip.Entry entry = it.next();
			if (entry.isText() && entry.getAsText() instanceof TooltipOverlayHandler.FoodOverlayTextComponent)
			{
				componentList.set(it.previousIndex(), ((TooltipOverlayHandler.FoodOverlayTextComponent) entry.getAsText()).foodOverlay);
			}
		}
	}

}
