package squeek.appleskin.mixin;

import net.minecraft.item.Item;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;

@Mixin(ItemStack.class)
public final class ItemStackMixin
{
	@Inject(at = @At("RETURN"), method = "getTooltip")
	private void getTooltipFromItem(final Item.TooltipContext context, final PlayerEntity player, final TooltipType type, final CallbackInfoReturnable<List<Text>> cir)
	{
		if (TooltipOverlayHandler.instance != null)
			TooltipOverlayHandler.instance.onItemTooltip((ItemStack) (Object) this, player, type, cir.getReturnValue());
	}
}
