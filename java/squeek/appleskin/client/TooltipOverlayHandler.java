package squeek.appleskin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import squeek.appleskin.helpers.FoodHelper;

public class TooltipOverlayHandler
{
	private static Identifier modIcons = new Identifier("appleskin", "textures/icons.png");
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_BOTTOM = 3;
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_TOP = -3;
	public static final int TOOLTIP_REAL_WIDTH_OFFSET_RIGHT = 3;

	public static void onRenderTooltip(ItemStack hoveredStack, int toolTipX, int toolTipY, int toolTipW, int toolTipH)
	{
		if (hoveredStack == null || hoveredStack.isEmpty())
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		Screen gui = mc.currentScreen;

		if (gui == null)
			return;

		if (!FoodHelper.isFood(hoveredStack))
			return;

		PlayerEntity player = mc.player;

		FoodHelper.BasicFoodValues defaultFoodValues = FoodHelper.getDefaultFoodValues(hoveredStack);
		FoodHelper.BasicFoodValues modifiedFoodValues = FoodHelper.getModifiedFoodValues(hoveredStack, player);

		if (defaultFoodValues.equals(modifiedFoodValues) && defaultFoodValues.hunger == 0)
			return;

		int biggestHunger = Math.max(defaultFoodValues.hunger, modifiedFoodValues.hunger);
		float biggestSaturationIncrement = Math.max(defaultFoodValues.getSaturationIncrement(), modifiedFoodValues.getSaturationIncrement());

		int barsNeeded = (int) Math.ceil(Math.abs(biggestHunger) / 2f);
		boolean hungerOverflow = barsNeeded > 10;
		String hungerText = hungerOverflow ? ((biggestHunger < 0 ? -1 : 1) * barsNeeded) + "x " : null;
		if (hungerOverflow)
			barsNeeded = 1;

		int saturationBarsNeeded = (int) Math.max(1, Math.ceil(Math.abs(biggestSaturationIncrement) / 2f));
		boolean saturationOverflow = saturationBarsNeeded > 10;
		String saturationText = saturationOverflow ? ((biggestSaturationIncrement < 0 ? -1 : 1) * saturationBarsNeeded) + "x " : null;
		if (saturationOverflow)
			saturationBarsNeeded = 1;

		int toolTipBottomY = toolTipY + toolTipH + 1 + TOOLTIP_REAL_HEIGHT_OFFSET_BOTTOM;
		int toolTipRightX = toolTipX + toolTipW + 1 + TOOLTIP_REAL_WIDTH_OFFSET_RIGHT;

		boolean shouldDrawBelow = toolTipBottomY + 20 < mc.window.getScaledHeight() - 3;

		int rightX = toolTipRightX - 3;
		int leftX = rightX - (Math.max(barsNeeded * 9 + (int) (mc.textRenderer.getStringWidth(hungerText) * 0.75f), saturationBarsNeeded * 6 + (int) (mc.textRenderer.getStringWidth(saturationText) * 0.75f))) - 3;
		int topY = (shouldDrawBelow ? toolTipBottomY : toolTipY - 20 + TOOLTIP_REAL_HEIGHT_OFFSET_TOP);
		int bottomY = topY + 19;

		GlStateManager.disableLighting();
		GlStateManager.disableDepthTest();

		// bg
		Screen.drawRect(leftX - 1, topY, rightX + 1, bottomY, 0xF0100010);
		Screen.drawRect(leftX, (shouldDrawBelow ? bottomY : topY - 1), rightX, (shouldDrawBelow ? bottomY + 1 : topY), 0xF0100010);
		Screen.drawRect(leftX, topY, rightX, bottomY, 0x66FFFFFF);

		// drawRect disables blending and modifies color, so reset them
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableBlend();
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		int x = rightX - 2;
		int startX = x;
		int y = bottomY - 18;

		mc.getTextureManager().bindTexture(Screen.ICONS);

		for (int i = 0; i < barsNeeded * 2; i += 2)
		{
			x -= 9;

			if (modifiedFoodValues.hunger < 0)
				gui.drawTexturedRect(x, y, 34, 27, 9, 9);
			else if (modifiedFoodValues.hunger > defaultFoodValues.hunger && defaultFoodValues.hunger <= i)
				gui.drawTexturedRect(x, y, 133, 27, 9, 9);
			else if (modifiedFoodValues.hunger > i + 1 || defaultFoodValues.hunger == modifiedFoodValues.hunger)
				gui.drawTexturedRect(x, y, 16, 27, 9, 9);
			else if (modifiedFoodValues.hunger == i + 1)
				gui.drawTexturedRect(x, y, 124, 27, 9, 9);
			else
				gui.drawTexturedRect(x, y, 34, 27, 9, 9);

			GlStateManager.color4f(1.0F, 1.0F, 1.0F, .25F);
			gui.drawTexturedRect(x, y, defaultFoodValues.hunger - 1 == i ? 115 : 106, 27, 9, 9);
			GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

			if (modifiedFoodValues.hunger > i)
				gui.drawTexturedRect(x, y, modifiedFoodValues.hunger - 1 == i ? 61 : 52, 27, 9, 9);
		}
		if (hungerText != null)
		{
			GlStateManager.pushMatrix();
			GlStateManager.scalef(0.75F, 0.75F, 0.75F);
			mc.textRenderer.drawWithShadow(hungerText, x * 4 / 3 - mc.textRenderer.getStringWidth(hungerText) + 2, y * 4 / 3 + 2, 0xFFDDDDDD);
			GlStateManager.popMatrix();
		}

		y += 10;
		x = startX;
		float modifiedSaturationIncrement = modifiedFoodValues.getSaturationIncrement();
		float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(modIcons);
		for (int i = 0; i < saturationBarsNeeded * 2; i += 2)
		{
			float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

			x -= 6;

			boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
			if (shouldBeFaded)
				GlStateManager.color4f(1.0F, 1.0F, 1.0F, .5F);

			gui.drawTexturedRect(x, y, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7);

			if (shouldBeFaded)
				GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		}
		if (saturationText != null)
		{
			GlStateManager.pushMatrix();
			GlStateManager.scalef(0.75F, 0.75F, 0.75F);
			mc.textRenderer.drawWithShadow(saturationText, x * 4 / 3 - mc.textRenderer.getStringWidth(saturationText) + 2, y * 4 / 3 + 1, 0xFFDDDDDD);
			GlStateManager.popMatrix();
		}

		GlStateManager.disableBlend();
		GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		// reset to drawHoveringText state
		GlStateManager.disableRescaleNormal();
		GuiLighting.disable();
		GlStateManager.disableLighting();
		GlStateManager.disableDepthTest();
	}
}
