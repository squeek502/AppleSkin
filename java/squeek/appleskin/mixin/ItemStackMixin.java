package squeek.appleskin.mixin;

import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import squeek.appleskin.client.LungePredictionHandler;
import squeek.appleskin.client.TooltipOverlayHandler;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin
{
	@Inject(at = @At("RETURN"), method = "getTooltip")
	private void getTooltipFromItem(Item.TooltipContext context, PlayerEntity player, TooltipType type, CallbackInfoReturnable<List<Text>> info)
	{
		if (TooltipOverlayHandler.INSTANCE != null)
			TooltipOverlayHandler.INSTANCE.onItemTooltip((ItemStack) (Object) this, player, context, type, info.getReturnValue());
	}

	// TEST SCAFFOLDING: predicts the saturation restored by finishing eating, to match the
	// same treatment Lunge gets in LungePredictionHandler. See that class for why the real
	// value can't be trusted on a server that doesn't run AppleSkin.
	@Inject(at = @At("HEAD"), method = "finishUsing")
	private void onFinishUsing(World world, LivingEntity user, CallbackInfoReturnable<ItemStack> info)
	{
		if (LungePredictionHandler.INSTANCE != null && user instanceof PlayerEntity player)
			LungePredictionHandler.INSTANCE.onFinishedEating((ItemStack) (Object) this, world, player);
	}
}
