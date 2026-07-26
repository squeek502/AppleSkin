package squeek.appleskin.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.client.LungePredictionHandler;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
	// TEST SCAFFOLDING: tickItemStackUsage only calls consumeItem() (which leads to
	// ItemStack#finishUsing) when !world.isClient() - the natural "finished eating" completion
	// is entirely server-only, so that's not a usable client hook. The itemUseTimeLeft
	// countdown itself, however, ticks identically on both sides, so hitting 0 right here is
	// the correct point to predict what finishing eating would have restored.
	@Inject(at = @At("TAIL"), method = "tickItemStackUsage")
	private void onTickItemStackUsage(ItemStack stack, CallbackInfo info)
	{
		LivingEntity self = (LivingEntity) (Object) this;
		if (self != MinecraftClient.getInstance().player || self.getItemUseTimeLeft() != 0)
			return;

		if (LungePredictionHandler.INSTANCE != null)
			LungePredictionHandler.INSTANCE.onFinishedEating(stack, self.getEntityWorld(), (PlayerEntity) self);
	}
}
