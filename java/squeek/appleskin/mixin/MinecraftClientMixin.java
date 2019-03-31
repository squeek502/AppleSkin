package squeek.appleskin.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.client.HUDOverlayHandler;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin
{
	@Inject(at = @At("HEAD"), method = "tick")
	void onTick(CallbackInfo info)
	{
		HUDOverlayHandler.onClientTick();
	}
}
