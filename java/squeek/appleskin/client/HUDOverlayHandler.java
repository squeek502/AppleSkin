package squeek.appleskin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffects;
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

import java.awt.geom.Point2D;
import java.util.Random;
import java.util.Vector;

public class HUDOverlayHandler
{
	private static float flashAlpha = 0f;
	private static byte alphaDir = 1;
	private static int foodIconsOffset;

	private static Random random = new Random();

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

		generateBarOffsets(matrixStack, mc.inGameHud.getTicks());

		if (shouldShowEstimatedHealth(heldItem))
		{
			float foodHealthIncrement = FoodHelper.getEstimatedHealthIncrement(heldItem, player);
			float modifiedHealth = Math.min(player.getHealth() + foodHealthIncrement, player.getMaxHealth());
			drawHealthOverlay(matrixStack, player.getHealth(), modifiedHealth, mc, 0, 0, flashAlpha);
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

		enableAlpha(alpha);
		mc.getTextureManager().bindTexture(modIcons);

		float modifiedSaturation = Math.min(saturationLevel + saturationGained, 20);

		int startSaturationBar = 0;
		int endSaturationBar = (int)Math.ceil(modifiedSaturation / 2.0F);

		if (saturationGained != 0)
			startSaturationBar = (int)Math.max(saturationLevel / 2.0F, 0);

		int iconSize = 9;

		for (int i = startSaturationBar; i < endSaturationBar; ++i)
		{
			// gets the location that needs to be render of icon
			Point2D location = foodBarOffsets.get(i);
			if (location == null)
				continue;

			int x = (int)location.getX() + 0;
			int y = (int)location.getY() + 0;

			int v = 0;
			int u = 0;

			float effectiveSaturationOfBar = (modifiedSaturation / 2.0F) - i;

			if (effectiveSaturationOfBar >= 1)
				u = 3 * iconSize;
			else if (effectiveSaturationOfBar > .5)
				u = 2 * iconSize;
			else if (effectiveSaturationOfBar > .25)
				u = 1 * iconSize;

			mc.inGameHud.drawTexture(matrixStack, x, y, u, v, iconSize, iconSize);
		}

		// rebind default icons
		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);
		disableAlpha(alpha);
	}

	public static void drawHungerOverlay(MatrixStack matrixStack, int hungerRestored, int foodLevel, MinecraftClient mc, int left, int top, float alpha, boolean useRottenTextures)
	{
		if (hungerRestored == 0)
			return;

		enableAlpha(alpha);
		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);

		int modifiedFood = Math.min(20, foodLevel + hungerRestored);

		int startFoodBars = foodLevel / 2;
		int endFoodBars = (int)Math.ceil(modifiedFood / 2.0F);

		int iconStartOffset = 16;
		int iconSize = 9;

		for (int i = startFoodBars; i < endFoodBars; ++i)
		{
			// gets the location that needs to be render of icon
			Point2D location = foodBarOffsets.get(i);
			if (location == null)
				continue;

			int x = (int)location.getX() + 0;
			int y = (int)location.getY() + 0;

			// location to normal food by default
			int v = 3 * iconSize;
			int u = iconStartOffset + 4 * iconSize;
			int ub = iconStartOffset + 1 * iconSize;

			// relocation to rotten food
			if (useRottenTextures)
			{
				u += 4 * iconSize;
				ub += 12 * iconSize;
			}

			// relocation to half food
			if (modifiedFood < (i + 1) * 2)
				u += 1 * iconSize;

			// very faint background
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha * 0.1F);
			mc.inGameHud.drawTexture(matrixStack, x, y, ub, v, iconSize, iconSize);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);

			mc.inGameHud.drawTexture(matrixStack, x, y, u, v, iconSize, iconSize);
		}

		disableAlpha(alpha);
	}

	public static void drawHealthOverlay(MatrixStack matrixStack, float health, float modifiedHealth, MinecraftClient mc, int offsetX, int offsetY, float alpha)
	{
		if (modifiedHealth <= health)
			return;

		enableAlpha(alpha);
		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);

		PlayerEntity player = mc.player;

		int startHealthBars = (int)Math.floor(Math.ceil(health) / 2.0F);
		int endHealthBars = (int)Math.ceil(modifiedHealth / 2.0F);

		int iconStartOffset = 16;
		int iconSize = 9;

		for (int i = startHealthBars; i < endHealthBars; ++i)
		{
			// gets the location that needs to be render of icon
			Point2D location = healthBarOffsets.get(i);
			if (location == null)
				continue;

			int x = (int)location.getX() + offsetX;
			int y = (int)location.getY() + offsetY;

			// location to full heart icon by default
			int v = 0 * iconSize;
			int u = iconStartOffset + 4 * iconSize;
			int ub = iconStartOffset + 1 * iconSize;

			// relocation to half heart
			if (modifiedHealth < (i + 1) * 2)
				u += 1 * iconSize;

			// relocation to special heart of hardcore
			if (player.world != null && player.world.getLevelProperties().isHardcore())
				v = 5 * iconSize;


			//// apply the status effects of the player
			//if (player.hasStatusEffect(StatusEffects.POISON)) {
			//	u += 4 * iconSize;
			//} else if (player.hasStatusEffect(StatusEffects.WITHER)) {
			//	u += 8 * iconSize;
			//}

			// very faint background
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha * 0.1F);
			mc.inGameHud.drawTexture(matrixStack, x, y, ub, v, iconSize, iconSize);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);

			mc.inGameHud.drawTexture(matrixStack, x, y, u, v, iconSize, iconSize);
		}

		disableAlpha(alpha);
	}

	public static void drawExhaustionOverlay(MatrixStack matrixStack, float exhaustion, MinecraftClient mc, int left, int top, float alpha)
	{
		mc.getTextureManager().bindTexture(modIcons);

		float maxExhaustion = HungerHelper.getMaxExhaustion(mc.player);
		// clamp between 0 and 1
		float ratio = Math.min(1, Math.max(0, exhaustion / maxExhaustion));
		int width = (int) (ratio * 81);
		int height = 9;

		enableAlpha(.75f);
		mc.inGameHud.drawTexture(matrixStack, left - width, top, 81 - width, 18, width, height);
		disableAlpha(.75f);

		// rebind default icons
		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);
	}



	private static void enableAlpha(float alpha)
	{
		RenderSystem.enableBlend();

		if (alpha == 1F)
			return;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
	}

	private static void disableAlpha(float alpha)
	{
		RenderSystem.disableBlend();

		if (alpha == 1F)
			return;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	public static void onClientTick()
	{
		flashAlpha += alphaDir * 0.125F;
		if (flashAlpha >= 1.5F)
		{
			flashAlpha = 1F;
			alphaDir = -1;
		}
		else if (flashAlpha <= -0.5F)
		{
			flashAlpha = 0F;
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

	private static boolean shouldShowEstimatedHealth(ItemStack hoveredStack)
	{
		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;
		HungerManager stats = player.getHungerManager();

		// when player is now naturally regeneration, player will confused how much of restored healths
		if (stats.getFoodLevel() >= 18)
			return false;

		if (player.hasStatusEffect(StatusEffects.POISON))
			return false;

		if (player.hasStatusEffect(StatusEffects.WITHER))
			return false;

		if (player.hasStatusEffect(StatusEffects.REGENERATION))
			return false;

		return true;
	}

	private static void generateBarOffsets(MatrixStack matrixStack, int ticks)
	{
		// hard code in `InGameHUD`
		random.setSeed((long)(ticks * 312871));

		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;
		HungerManager hungerManager = player.getHungerManager();

		int left = mc.getWindow().getScaledWidth() / 2 - 91; // left of health bar
		int right = mc.getWindow().getScaledWidth() / 2 + 91; // right of food bar
		int bottom = mc.getWindow().getScaledHeight() - foodIconsOffset;

		float maxHealth = (float)player.getAttributeValue(EntityAttributes.GENERIC_MAX_HEALTH);
		float absorptionHealth = (float)Math.ceil(player.getAbsorptionAmount());

		int healthBars = (int)Math.ceil((maxHealth + absorptionHealth) / 2.0F);
		int healthRows = (int)Math.ceil((float)healthBars / 10.0F);

		int heightForHealth = Math.max(10 - (healthRows - 2), 3);

		int foodBars = 10;

		// adjust the size
		if (healthBarOffsets.size() != healthBars)
			healthBarOffsets.setSize(healthBars);

		if (foodBarOffsets.size() != foodBars)
			foodBarOffsets.setSize(foodBars);

		// when health is below 5 will random move the health icon
		boolean shouldAnimatedHealth = Math.ceil(player.getHealth()) <= 4;
		for (int i = healthBars - 1; i >= 0; --i)
		{
			int row = (int)Math.ceil((float)(i + 1) / 10.0F) - 1;
			int x = left + i % 10 * 8;
			int y = bottom - row * heightForHealth;
			// apply the animated offset
			if (shouldAnimatedHealth)
				y += random.nextInt(2);

			Point2D point = new Point2D.Float(x, y);
			healthBarOffsets.set(i, point);
		}

		// when saturation level is zero will random move the food icon
		float saturationLevel = hungerManager.getSaturationLevel();
		int foodLevel = hungerManager.getFoodLevel();
		boolean shouldAnimatedFood = saturationLevel <= 0.0F && ticks % (foodLevel * 3 + 1) == 0;
		for(int i = 0; i < foodBars; ++i)
		{
			int x = right - i * 8 - 9;
			int y = bottom;
			// apply the animated offset
			if (shouldAnimatedFood)
				y += random.nextInt(3) - 1;

			Point2D point = new Point2D.Float(x, y);
			foodBarOffsets.set(i, point);
		}

		// test
		for (Point2D point : healthBarOffsets)
		{
			int x = (int)point.getX();
			int y = (int)point.getY();
			Screen.fill(matrixStack, x, y, x + 9, y + 9, 0x0fff0000);
		}
		for (Point2D point : foodBarOffsets)
		{
			int x = (int)point.getX();
			int y = (int)point.getY();
			Screen.fill(matrixStack, x, y, x + 9, y + 9, 0x0fff0000);
		}
	}

	private static Vector<Point2D> healthBarOffsets = new Vector<>();
	private static Vector<Point2D> foodBarOffsets = new Vector<>();
}
