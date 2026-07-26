package squeek.appleskin.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.client.HUDOverlayHandler;
import squeek.appleskin.client.LungePredictionHandler;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin
{
	@Inject(at = @At("HEAD"), method = "tick")
	void onTick(CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onClientTick();
	}

	// TEST SCAFFOLDING: piercing weapons (spears) dispatch their jab attack through this call
	// unconditionally, whether or not it hits anything - unlike normal weapons, which only
	// reach ClientPlayerInteractionManager#attackEntity/attackBlock when the crosshair target
	// is actually an entity/block. This is the only point that fires for movement-only lunges.
	@Inject(
		method = "doAttack",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;attackWithPiercingWeapon(Lnet/minecraft/component/type/PiercingWeaponComponent;)V"
		)
	)
	void onPiercingAttack(CallbackInfo info)
	{
		if (LungePredictionHandler.INSTANCE != null)
			LungePredictionHandler.INSTANCE.onSwingAttempt();
	}
}
