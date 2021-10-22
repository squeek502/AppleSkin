package squeek.appleskin.mixin;

import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import squeek.appleskin.client.TooltipOverlayHandler;

@Mixin(TooltipComponent.class)
public interface TooltipComponentMixin extends TooltipComponent
{
	// This allows AppleSkin to add its tooltip as an OrderedText, which gets converted
	// into our custom TooltipComponent implementation during TooltipComponent::of

	@Inject(
		at = @At("HEAD"),
		method = "of(Lnet/minecraft/text/OrderedText;)Lnet/minecraft/client/gui/tooltip/TooltipComponent;",
		cancellable = true
	)
	private static void AppleSkin_of(OrderedText text, CallbackInfoReturnable<TooltipComponent> info)
	{
		if (text instanceof TooltipOverlayHandler.FoodOverlayTextComponent)
		{
			info.setReturnValue(((TooltipOverlayHandler.FoodOverlayTextComponent) text).foodOverlay);
		}
	}
}
