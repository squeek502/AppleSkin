package squeek.appleskin.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin
{
	@Inject(at = @At("RETURN"), method = "getTooltipLines")
	private void getTooltipFromItem(Item.TooltipContext context, Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> info)
	{
		if (TooltipOverlayHandler.INSTANCE != null)
			TooltipOverlayHandler.INSTANCE.onItemTooltip((ItemStack) (Object) this, player, context, type, info.getReturnValue());
	}
}
