package squeek.appleskin.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.client.HUDOverlayHandler;

@Mixin(Gui.class)
public class GuiMixin
{
	@Inject(at = @At("HEAD"), method = "extractFood")
	private void renderFoodPre(GuiGraphicsExtractor context, Player player, int top, int right, CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onPreRenderFood(context, player, top, right);
	}

	@Inject(at = @At("RETURN"), method = "extractFood")
	private void renderFoodPost(GuiGraphicsExtractor context, Player player, int top, int right, CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onRenderFood(context, player, top, right);
	}

	@Inject(at = @At("RETURN"), method = "extractHearts")
	private void renderHealthPost(GuiGraphicsExtractor context, Player player, int left, int top, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking, CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onRenderHealth(context, player, left, top, lines, regeneratingHeartIndex, maxHealth, lastHealth, health, absorption, blinking);
	}
}
