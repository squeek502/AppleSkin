package squeek.appleskin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.opengl.GL11;
import squeek.appleskin.ModConfig;
import squeek.appleskin.ModInfo;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.KeyHelper;

@OnlyIn(Dist.CLIENT)
public class TooltipOverlayHandler
{
	private static ResourceLocation modIcons = new ResourceLocation(ModInfo.MODID_LOWER, "textures/icons.png");
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_BOTTOM = 3;
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_TOP = -3;
	public static final int TOOLTIP_REAL_WIDTH_OFFSET_RIGHT = 3;

	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new TooltipOverlayHandler());
	}

	//These are the texture locations that should be used for normal food
	final int[] normalBarTextures = new int[] {43, 133, 16, 124, 34, 115, 106, 61, 52};
	//These are the texture locations that should be used for rotten food
	final int[] rottenBarTextures = new int[] {43, 133, 16, 133, 34, 115, 106, 97, 87};

	@SubscribeEvent
	public void onRenderTooltip(RenderTooltipEvent.PostText event)
	{
		ItemStack hoveredStack = event.getStack();
		if (hoveredStack.isEmpty())
			return;

		boolean shouldShowTooltip = (ModConfig.SHOW_FOOD_VALUES_IN_TOOLTIP.get() && KeyHelper.isShiftKeyDown()) || ModConfig.ALWAYS_SHOW_FOOD_VALUES_TOOLTIP.get();
		if (!shouldShowTooltip)
			return;

		Minecraft mc = Minecraft.getInstance();
		Screen gui = mc.currentScreen;

		if (gui == null)
			return;

		if (!FoodHelper.isFood(hoveredStack))
			return;

		PlayerEntity player = mc.player;
		int toolTipY = event.getY();
		int toolTipX = event.getX();
		int toolTipW = event.getWidth();
		int toolTipH = event.getHeight();

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

		boolean shouldDrawBelow = toolTipBottomY + 20 < mc.func_228018_at_().getScaledHeight() - 3;

		int rightX = toolTipRightX - 3;
		int leftX = rightX - (Math.max(barsNeeded * 9 + (int) (mc.fontRenderer.getStringWidth(hungerText) * 0.75f), saturationBarsNeeded * 6 + (int) (mc.fontRenderer.getStringWidth(saturationText) * 0.75f))) - 3;
		int topY = (shouldDrawBelow ? toolTipBottomY : toolTipY - 20 + TOOLTIP_REAL_HEIGHT_OFFSET_TOP);
		int bottomY = topY + 19;

		RenderSystem.disableLighting();
		RenderSystem.disableDepthTest();

		// bg
		AbstractGui.fill(leftX - 1, topY, rightX + 1, bottomY, 0xF0100010);
		AbstractGui.fill(leftX, (shouldDrawBelow ? bottomY : topY - 1), rightX, (shouldDrawBelow ? bottomY + 1 : topY), 0xF0100010);
		AbstractGui.fill(leftX, topY, rightX, bottomY, 0x66FFFFFF);

		// drawRect disables blending and modifies color, so reset them
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.enableBlend();
		RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

		int x = rightX - 2;
		int startX = x;
		int y = bottomY - 18;

		mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);

		int[] bars = FoodHelper.isRotten(hoveredStack) ? rottenBarTextures : normalBarTextures;
		for (int i = 0; i < barsNeeded * 2; i += 2)
		{
			x -= 9;

			if (modifiedFoodValues.hunger < 0)
				gui.blit(x, y, bars[0], 27, 9, 9);
			else if (modifiedFoodValues.hunger > defaultFoodValues.hunger && defaultFoodValues.hunger <= i)
				gui.blit(x, y, bars[1], 27, 9, 9);
			else if (modifiedFoodValues.hunger > i + 1 || defaultFoodValues.hunger == modifiedFoodValues.hunger)
				gui.blit(x, y, bars[2], 27, 9, 9);
			else if (modifiedFoodValues.hunger == i + 1)
				gui.blit(x, y, bars[3], 27, 9, 9);
			else
				gui.blit(x, y, bars[4], 27, 9, 9);

			RenderSystem.color4f(1.0F, 1.0F, 1.0F, .25F);
			gui.blit(x, y, defaultFoodValues.hunger - 1 == i ? bars[5] : bars[6], 27, 9, 9);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

			if (modifiedFoodValues.hunger > i)
				gui.blit(x, y, modifiedFoodValues.hunger - 1 == i ? bars[7] : bars[8], 27, 9, 9);
		}
		if (hungerText != null)
		{
			RenderSystem.pushMatrix();
			RenderSystem.scalef(0.75F, 0.75F, 0.75F);
			mc.fontRenderer.drawStringWithShadow(hungerText, x * 4 / 3 - mc.fontRenderer.getStringWidth(hungerText) + 2, y * 4 / 3 + 2, 0xFFDDDDDD);
			RenderSystem.popMatrix();
		}

		y += 10;
		x = startX;
		float modifiedSaturationIncrement = modifiedFoodValues.getSaturationIncrement();
		float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(modIcons);
		for (int i = 0; i < saturationBarsNeeded * 2; i += 2)
		{
			float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

			x -= 6;

			boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
			if (shouldBeFaded)
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, .5F);

			gui.blit(x, y, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7);

			if (shouldBeFaded)
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		}
		if (saturationText != null)
		{
			RenderSystem.pushMatrix();
			RenderSystem.scalef(0.75F, 0.75F, 0.75F);
			mc.fontRenderer.drawStringWithShadow(saturationText, x * 4 / 3 - mc.fontRenderer.getStringWidth(saturationText) + 2, y * 4 / 3 + 1, 0xFFDDDDDD);
			RenderSystem.popMatrix();
		}

		RenderSystem.disableBlend();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		// reset to drawHoveringText state
		RenderSystem.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
		RenderSystem.disableLighting();
		RenderSystem.disableDepthTest();
	}
}
