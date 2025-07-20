package squeek.appleskin.mixin;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.client.HUDOverlayHandler;

@Mixin(InGameHud.class)
public final class InGameHudMixin
{
	@Inject(at = @At("HEAD"), method = "renderFood")
	private void renderFoodPre(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo info)
	{
		if (HUDOverlayHandler.instance != null)
			HUDOverlayHandler.instance.onPreRenderFood(context, player, top, right);
	}

	@Inject(at = @At("RETURN"), method = "renderFood")
	private void renderFoodPost(DrawContext context, PlayerEntity player, int top, int right, CallbackInfo info)
	{
		if (HUDOverlayHandler.instance != null)
			HUDOverlayHandler.instance.onRenderFood(context, player, top, right);
	}

	@Inject(at = @At("RETURN"), method = "renderHealthBar")
	private void renderHealthPost(final DrawContext context, final PlayerEntity player, final int x, final int y, final int lines, final int regeneratingHeartIndex, final float maxHealth, final int lastHealth, final int health, final int absorption, final boolean blinking, final CallbackInfo ci)
	{
		if (HUDOverlayHandler.instance != null)
			HUDOverlayHandler.instance.onRenderHealth(context, player, x, y);
	}
}
