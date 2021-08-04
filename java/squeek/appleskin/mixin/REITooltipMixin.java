package squeek.appleskin.mixin;

import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.impl.client.gui.fabric.ScreenOverlayImplImpl;
import net.minecraft.client.gui.screen.Screen;
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

@Mixin(value = ScreenOverlayImplImpl.class, remap = false)
public class REITooltipMixin
{
	// See the comment in ScreenMixin for a complete explanation.
	//
	// This should be seen as a workaround until its possible to mixin TooltipComponent::of.
	// (i.e. once https://github.com/FabricMC/Mixin/pull/51 makes it into a Fabric build)

	@Inject(
		at = @At(value = "INVOKE", target = "Lme/shedaniel/rei/impl/client/gui/fabric/ScreenOverlayImplImpl;renderTooltipInner", ordinal = 0),
		method = "renderTooltipInner",
		locals = LocalCapture.CAPTURE_FAILHARD
	)
	private static void onRenderTooltip(Screen screen, MatrixStack matrices, Tooltip tooltip, int mouseX, int mouseY, CallbackInfo info, List<TooltipComponent> componentList)
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
