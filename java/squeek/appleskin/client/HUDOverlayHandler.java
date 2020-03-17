package squeek.appleskin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.HungerHelper;

public class HUDOverlayHandler
{
	private static float flashAlpha = 0f;
	private static byte alphaDir = 1;
	private static int foodIconsOffset;
	public static int FOOD_BAR_HEIGHT = 39;

	private static final Identifier modIcons = new Identifier("appleskin", "textures/icons.png");

	public static void onPreRender()
	{
		foodIconsOffset = FOOD_BAR_HEIGHT;

		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;

		int left = mc.getWindow().getScaledWidth() / 2 + 91;
		int top = mc.getWindow().getScaledHeight() - foodIconsOffset;

		drawExhaustionOverlay(HungerHelper.getExhaustion(player), mc, left, top, 1f);
	}

	public static void onRender()
	{
		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;
		ItemStack heldItem = player.getMainHandStack();
		HungerManager stats = player.getHungerManager();

		int left = mc.getWindow().getScaledWidth() / 2 + 91;
		int top = mc.getWindow().getScaledHeight() - foodIconsOffset;

		// saturation overlay
		drawSaturationOverlay(0, stats.getSaturationLevel(), mc, left, top, 1f);

		if (heldItem.isEmpty() || !FoodHelper.isFood(heldItem))
		{
			flashAlpha = 0;
			alphaDir = 1;
			return;
		}

		// restored hunger/saturation overlay while holding food
		FoodHelper.BasicFoodValues foodValues = FoodHelper.getModifiedFoodValues(heldItem, player);
		drawHungerOverlay(foodValues.hunger, stats.getFoodLevel(), mc, left, top, flashAlpha);

		int newFoodValue = stats.getFoodLevel() + foodValues.hunger;
		float newSaturationValue = stats.getSaturationLevel() + foodValues.getSaturationIncrement();
		drawSaturationOverlay(newSaturationValue > newFoodValue ? newFoodValue - stats.getSaturationLevel() : foodValues.getSaturationIncrement(), stats.getSaturationLevel(), mc, left, top, flashAlpha);
	}

	public static void drawSaturationOverlay(float saturationGained, float saturationLevel, MinecraftClient mc, int left, int top, float alpha)
	{
		if (saturationLevel + saturationGained < 0)
			return;

		int startBar = saturationGained != 0 ? Math.max(0, (int) saturationLevel / 2) : 0;
		int endBar = (int) Math.ceil(Math.min(20, saturationLevel + saturationGained) / 2f);
		int barsNeeded = endBar - startBar;
		mc.getTextureManager().bindTexture(modIcons);

		enableAlpha(alpha);
		for (int i = startBar; i < startBar + barsNeeded; ++i)
		{
			int x = left - i * 8 - 9;
			int y = top;
			float effectiveSaturationOfBar = (saturationLevel + saturationGained) / 2 - i;

			if (effectiveSaturationOfBar >= 1)
				mc.inGameHud.drawTexture(x, y, 27, 0, 9, 9);
			else if (effectiveSaturationOfBar > .5)
				mc.inGameHud.drawTexture(x, y, 18, 0, 9, 9);
			else if (effectiveSaturationOfBar > .25)
				mc.inGameHud.drawTexture(x, y, 9, 0, 9, 9);
			else if (effectiveSaturationOfBar > 0)
				mc.inGameHud.drawTexture(x, y, 0, 0, 9, 9);
		}
		disableAlpha(alpha);

		// rebind default icons
		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);
	}

	public static void drawHungerOverlay(int hungerRestored, int foodLevel, MinecraftClient mc, int left, int top, float alpha)
	{
		if (hungerRestored == 0)
			return;

		int startBar = foodLevel / 2;
		int endBar = (int) Math.ceil(Math.min(20, foodLevel + hungerRestored) / 2f);
		int barsNeeded = endBar - startBar;
		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);

		enableAlpha(alpha);
		for (int i = startBar; i < startBar + barsNeeded; ++i)
		{
			int idx = i * 2 + 1;
			int x = left - i * 8 - 9;
			int y = top;
			int icon = 16;
			int background = 13;

			if (mc.player.hasStatusEffect(StatusEffects.HUNGER))
			{
				icon += 36;
				background = 13;
			}

			mc.inGameHud.drawTexture(x, y, 16 + background * 9, 27, 9, 9);

			if (idx < foodLevel + hungerRestored)
				mc.inGameHud.drawTexture(x, y, icon + 36, 27, 9, 9);
			else if (idx == foodLevel + hungerRestored)
				mc.inGameHud.drawTexture(x, y, icon + 45, 27, 9, 9);
		}
		disableAlpha(alpha);
	}

	public static void drawExhaustionOverlay(float exhaustion, MinecraftClient mc, int left, int top, float alpha)
	{
		mc.getTextureManager().bindTexture(modIcons);

		float maxExhaustion = HungerHelper.getMaxExhaustion(mc.player);
		float ratio = exhaustion / maxExhaustion;
		int width = (int) (ratio * 81);
		int height = 9;

		enableAlpha(.75f);
		mc.inGameHud.drawTexture(left - width, top, 81 - width, 18, width, height);
		disableAlpha(.75f);

		// rebind default icons
		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);
	}

	private static void enableAlpha(float alpha)
	{
		RenderSystem.enableBlend();

		if (alpha == 1f)
			return;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
	}

	private static void disableAlpha(float alpha)
	{
		RenderSystem.disableBlend();

		if (alpha == 1f)
			return;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	public static void onClientTick()
	{
		flashAlpha += alphaDir * 0.125f;
		if (flashAlpha >= 1.5f)
		{
			flashAlpha = 1f;
			alphaDir = -1;
		}
		else if (flashAlpha <= -0.5f)
		{
			flashAlpha = 0f;
			alphaDir = 1;
		}
	}
}
