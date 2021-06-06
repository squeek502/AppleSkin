package squeek.appleskin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.HungerHelper;

public class HUDOverlayHandler
{
	private static float flashAlpha = 0f;
	private static byte alphaDir = 1;
	private static int foodIconsOffset;
	public static int FOOD_BAR_HEIGHT = 39;

	private static final Identifier modIcons = new Identifier("appleskin", "textures/icons.png");

	public static void onPreRender(MatrixStack matrixStack)
	{
		foodIconsOffset = FOOD_BAR_HEIGHT;

		if (!ModConfig.INSTANCE.showFoodExhaustionHudUnderlay)
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;

		int left = mc.getWindow().getScaledWidth() / 2 + 91;
		int top = mc.getWindow().getScaledHeight() - foodIconsOffset;
		float exhaustion = HungerHelper.getExhaustion(player);

		// Notify everyone that we should render exhaustion hud overlay
		HUDOverlayEvent.Exhaustion renderEvent = new HUDOverlayEvent.Exhaustion(exhaustion, left, top, matrixStack);
		HUDOverlayEvent.Exhaustion.EVENT.invoker().interact(renderEvent);
		if (!renderEvent.isCanceled)
		{
			drawExhaustionOverlay(renderEvent, 1f, mc);
		}
	}

	public static void onRender(MatrixStack matrixStack)
	{
		if (!ModConfig.INSTANCE.showFoodValuesHudOverlay && !ModConfig.INSTANCE.showSaturationHudOverlay)
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;
		HungerManager stats = player.getHungerManager();

		int left = mc.getWindow().getScaledWidth() / 2 + 91;
		int top = mc.getWindow().getScaledHeight() - foodIconsOffset;
		float saturationLevel = stats.getSaturationLevel();

		// Notify everyone that we should render saturation hud overlay
		HUDOverlayEvent.Saturation saturationRenderEvent = new HUDOverlayEvent.Saturation(saturationLevel, left, top, matrixStack);

		// Cancel render overlay event when configuration disabled.
		if (!ModConfig.INSTANCE.showSaturationHudOverlay)
		{
			saturationRenderEvent.isCanceled = true;
		}

		// Notify everyone that we should render saturation hud overlay
		if (!saturationRenderEvent.isCanceled)
		{
			HUDOverlayEvent.Saturation.EVENT.invoker().interact(saturationRenderEvent);
		}

		// Draw saturation overlay
		if (!saturationRenderEvent.isCanceled)
		{
			drawSaturationOverlay(saturationRenderEvent, 0, 1f, mc);
		}

		ItemStack heldItem = player.getMainHandStack();
		if (ModConfig.INSTANCE.showFoodValuesHudOverlayWhenOffhand && !FoodHelper.isFood(heldItem))
		{
			heldItem = player.getOffHandStack();
		}

		if (!ModConfig.INSTANCE.showFoodValuesHudOverlay || heldItem.isEmpty() || !FoodHelper.isFood(heldItem))
		{
			resetFlash();
			return;
		}

		// restored hunger/saturation overlay while holding food
		int foodLevel = stats.getFoodLevel();
		FoodValues modifiedFood = FoodHelper.getModifiedFoodValues(heldItem, player);

		FoodValuesEvent foodValuesEvent = new FoodValuesEvent(player, heldItem, FoodHelper.getDefaultFoodValues(heldItem), modifiedFood);
		FoodValuesEvent.EVENT.invoker().interact(foodValuesEvent);
		modifiedFood = foodValuesEvent.modifiedFoodValues;

		// Notify everyone that we should render hunger hud overlay
		HUDOverlayEvent.HungerRestored hungerRenderEvent = new HUDOverlayEvent.HungerRestored(foodLevel, heldItem, modifiedFood, left, top, matrixStack);
		HUDOverlayEvent.HungerRestored.EVENT.invoker().interact(hungerRenderEvent);
		if (hungerRenderEvent.isCanceled)
		{
			resetFlash();
			return;
		}

		// Calculate the final hunger and saturation
		int foodHunger = modifiedFood.hunger;
		float foodSaturationIncrement = modifiedFood.getSaturationIncrement();

		// Draw hunger overlay
		drawHungerOverlay(hungerRenderEvent, foodHunger, flashAlpha, FoodHelper.isRotten(heldItem), mc);

		int newFoodValue = stats.getFoodLevel() + foodHunger;
		float newSaturationValue = saturationLevel + foodSaturationIncrement;

		// Draw saturation overlay of gained
		if (!saturationRenderEvent.isCanceled)
		{
			float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - saturationLevel : foodSaturationIncrement;
			drawSaturationOverlay(saturationRenderEvent, saturationGained, flashAlpha, mc);
		}
	}

	public static void drawSaturationOverlay(MatrixStack matrixStack, float saturationGained, float saturationLevel, MinecraftClient mc, int left, int top, float alpha)
	{
		if (saturationLevel + saturationGained < 0)
			return;

		int startBar = saturationGained != 0 ? Math.max(0, (int) saturationLevel / 2) : 0;
		int endBar = (int) Math.ceil(Math.min(20, saturationLevel + saturationGained) / 2f);
		int barsNeeded = endBar - startBar;
		RenderSystem.setShaderTexture(0, modIcons);

		enableAlpha(alpha);
		for (int i = startBar; i < startBar + barsNeeded; ++i)
		{
			int x = left - i * 8 - 9;
			int y = top;
			float effectiveSaturationOfBar = (saturationLevel + saturationGained) / 2 - i;

			if (effectiveSaturationOfBar >= 1)
				mc.inGameHud.drawTexture(matrixStack, x, y, 27, 0, 9, 9);
			else if (effectiveSaturationOfBar > .5)
				mc.inGameHud.drawTexture(matrixStack, x, y, 18, 0, 9, 9);
			else if (effectiveSaturationOfBar > .25)
				mc.inGameHud.drawTexture(matrixStack, x, y, 9, 0, 9, 9);
			else if (effectiveSaturationOfBar > 0)
				mc.inGameHud.drawTexture(matrixStack, x, y, 0, 0, 9, 9);
		}
		disableAlpha(alpha);

		// rebind default icons
		RenderSystem.setShaderTexture(0, Screen.GUI_ICONS_TEXTURE);
	}

	public static void drawHungerOverlay(MatrixStack matrixStack, int hungerRestored, int foodLevel, MinecraftClient mc, int left, int top, float alpha, boolean useRottenTextures)
	{
		if (hungerRestored == 0)
			return;

		int startBar = foodLevel / 2;
		int endBar = (int) Math.ceil(Math.min(20, foodLevel + hungerRestored) / 2f);
		int barsNeeded = endBar - startBar;
		RenderSystem.setShaderTexture(0, Screen.GUI_ICONS_TEXTURE);

		enableAlpha(alpha);
		for (int i = startBar; i < startBar + barsNeeded; ++i)
		{
			int idx = i * 2 + 1;
			int x = left - i * 8 - 9;
			int y = top;
			int icon = 16;
			int background = 1;

			if (useRottenTextures)
			{
				icon += 36;
				background = 13;
			}

			// very faint background
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * 0.1f);
			mc.inGameHud.drawTexture(matrixStack, x, y, 16 + background * 9, 27, 9, 9);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

			if (idx < foodLevel + hungerRestored)
				mc.inGameHud.drawTexture(matrixStack, x, y, icon + 36, 27, 9, 9);
			else if (idx == foodLevel + hungerRestored)
				mc.inGameHud.drawTexture(matrixStack, x, y, icon + 45, 27, 9, 9);
		}
		disableAlpha(alpha);
	}

	public static void drawExhaustionOverlay(MatrixStack matrixStack, float exhaustion, MinecraftClient mc, int left, int top, float alpha)
	{
		RenderSystem.setShaderTexture(0, modIcons);

		float maxExhaustion = HungerHelper.getMaxExhaustion(mc.player);
		// clamp between 0 and 1
		float ratio = Math.min(1, Math.max(0, exhaustion / maxExhaustion));
		int width = (int) (ratio * 81);
		int height = 9;

		enableAlpha(.75f);
		mc.inGameHud.drawTexture(matrixStack, left - width, top, 81 - width, 18, width, height);
		disableAlpha(.75f);

		// rebind default icons
		RenderSystem.setShaderTexture(0, Screen.GUI_ICONS_TEXTURE);
	}

	private static void enableAlpha(float alpha)
	{
		RenderSystem.enableBlend();

		if (alpha == 1f)
			return;

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
	}

	private static void disableAlpha(float alpha)
	{
		RenderSystem.disableBlend();

		if (alpha == 1f)
			return;

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
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

	public static void resetFlash()
	{
		flashAlpha = 0;
		alphaDir = 1;
	}

	private static void drawExhaustionOverlay(HUDOverlayEvent.Exhaustion event, float alpha, MinecraftClient mc)
	{
		drawExhaustionOverlay(event.matrixStack, event.exhaustion, mc, event.x, event.y, alpha);
	}

	private static void drawSaturationOverlay(HUDOverlayEvent.Saturation event, float saturationGained, float alpha, MinecraftClient mc)
	{
		drawSaturationOverlay(event.matrixStack, saturationGained, event.saturationLevel, mc, event.x, event.y, alpha);
	}

	private static void drawHungerOverlay(HUDOverlayEvent.HungerRestored event, int hunger, float alpha, boolean useRottenTextures, MinecraftClient mc)
	{
		drawHungerOverlay(event.matrixStack, hunger, event.currentFoodLevel, mc, event.x, event.y, alpha, useRottenTextures);
	}
}
