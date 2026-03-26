package squeek.appleskin.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
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
	private void renderFoodPre(GuiGraphicsExtractor graphics, Player player, int top, int right, CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onPreRenderFood(graphics, player, top, right);
	}

	@Inject(at = @At("RETURN"), method = "extractFood")
	private void renderFoodPost(GuiGraphicsExtractor graphics, Player player, int top, int right, CallbackInfo info)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onRenderFood(graphics, player, top, right);
	}

	@Inject(at = @At("RETURN"), method = "extractHearts")
	private void renderHealthPost(GuiGraphicsExtractor graphics, Player player, int xLeft, int yLineBase, int healthRowHeight, int heartOffsetIndex, float maxHealth, int currentHealth, int oldHealth, int absorption, boolean blink, CallbackInfo ci)
	{
		if (HUDOverlayHandler.INSTANCE != null)
			HUDOverlayHandler.INSTANCE.onRenderHealth(graphics, player, xLeft, yLineBase);
	}
}
