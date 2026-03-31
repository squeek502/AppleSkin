package squeek.appleskin.mixin;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import squeek.appleskin.client.TooltipOverlayHandler;

@Mixin(ClientTooltipComponent.class)
public interface ClientTooltipComponentMixin extends ClientTooltipComponent
{
	// This allows AppleSkin to add its tooltip as an OrderedText, which gets converted
	// into our custom TooltipComponent implementation during TooltipComponent::of
	@Inject(
		at = @At("HEAD"),
		method = "create(Lnet/minecraft/util/FormattedCharSequence;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
		cancellable = true
	)
	private static void AppleSkin_of(FormattedCharSequence text, CallbackInfoReturnable<ClientTooltipComponent> info)
	{
		if (text instanceof TooltipOverlayHandler.FoodOverlayTextComponent)
		{
			info.setReturnValue(((TooltipOverlayHandler.FoodOverlayTextComponent) text).foodOverlay);
		}
	}

	// Also allow for TooltipData conversion, needed for REI compatibility since we do
	// OrderedText -> TooltipData -> TooltipComponent for REI
	@Inject(
		at = @At("HEAD"),
		method = "create(Lnet/minecraft/world/inventory/tooltip/TooltipComponent;)Lnet/minecraft/client/gui/screens/inventory/tooltip/ClientTooltipComponent;",
		cancellable = true
	)
	private static void AppleSkin_ofData(TooltipComponent data, CallbackInfoReturnable<ClientTooltipComponent> info)
	{
		if (data instanceof TooltipOverlayHandler.FoodOverlay)
		{
			info.setReturnValue((TooltipOverlayHandler.FoodOverlay) data);
		}
	}
}
