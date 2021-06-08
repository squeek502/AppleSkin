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
import net.minecraft.world.Difficulty;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.HungerHelper;
import squeek.appleskin.util.IntPoint;

import java.util.Random;
import java.util.Vector;

public class HUDOverlayHandler
{
	private static float flashAlpha = 0f;
	private static byte alphaDir = 1;
	private static int foodIconsOffset;

	public static int FOOD_BAR_HEIGHT = 39;

	public static final Vector<IntPoint> healthBarOffsets = new Vector<>();
	public static final Vector<IntPoint> foodBarOffsets = new Vector<>();

	private static final Random random = new Random();
	private static final Identifier modIcons = new Identifier("appleskin", "textures/icons.png");


	public static void onPreRender(MatrixStack matrixStack)
	{
		foodIconsOffset = FOOD_BAR_HEIGHT;

		if (!ModConfig.INSTANCE.showFoodExhaustionHudUnderlay)
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;

		int right = mc.getWindow().getScaledWidth() / 2 + 91;
		int top = mc.getWindow().getScaledHeight() - foodIconsOffset;
		float exhaustion = HungerHelper.getExhaustion(player);

		// Notify everyone that we should render exhaustion hud overlay
		HUDOverlayEvent.Exhaustion renderEvent = new HUDOverlayEvent.Exhaustion(exhaustion, right, top, matrixStack);
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

		int top = mc.getWindow().getScaledHeight() - foodIconsOffset;
		int left = mc.getWindow().getScaledWidth() / 2 - 91; // left of health bar
		int right = mc.getWindow().getScaledWidth() / 2 + 91; // right of food bar

		// generate at the beginning to avoid ArrayIndexOutOfBoundsException
		generateBarOffsets(top, left, right, mc.inGameHud.getTicks());

		// notify everyone that we should render saturation hud overlay
		float saturationLevel = stats.getSaturationLevel();
		HUDOverlayEvent.Saturation saturationRenderEvent = new HUDOverlayEvent.Saturation(saturationLevel, right, top, matrixStack);

		// cancel render overlay event when configuration disabled.
		if (!ModConfig.INSTANCE.showSaturationHudOverlay)
			saturationRenderEvent.isCanceled = true;

		// notify everyone that we should render saturation hud overlay
		if (!saturationRenderEvent.isCanceled)
			HUDOverlayEvent.Saturation.EVENT.invoker().interact(saturationRenderEvent);

		// draw saturation overlay
		if (!saturationRenderEvent.isCanceled)
			drawSaturationOverlay(saturationRenderEvent, 0, 1f, mc);

		// try to get the item stack in the player hand
		ItemStack heldItem = player.getMainHandStack();
		if (ModConfig.INSTANCE.showFoodValuesHudOverlayWhenOffhand && !FoodHelper.isFood(heldItem))
			heldItem = player.getOffHandStack();

		// showFoodValuesHudOverlay will control all overlays based on food item
		if (!ModConfig.INSTANCE.showFoodValuesHudOverlay || heldItem.isEmpty() || !FoodHelper.isFood(heldItem))
		{
			resetFlash();
			return;
		}

		// restored hunger/saturation overlay while holding food
		int foodLevel = stats.getFoodLevel();
		FoodValues modifiedFoodValues = FoodHelper.getModifiedFoodValues(heldItem, player);

		FoodValuesEvent foodValuesEvent = new FoodValuesEvent(player, heldItem, FoodHelper.getDefaultFoodValues(heldItem), modifiedFoodValues);
		FoodValuesEvent.EVENT.invoker().interact(foodValuesEvent);
		modifiedFoodValues = foodValuesEvent.modifiedFoodValues;

		// draw health overlay
		if (shouldShowEstimatedHealth(heldItem, modifiedFoodValues))
		{
			float foodHealthIncrement = FoodHelper.getEstimatedHealthIncrement(heldItem, modifiedFoodValues, player);
			float modifiedHealth = Math.min(player.getHealth() + foodHealthIncrement, player.getMaxHealth());
			drawHealthOverlay(matrixStack, player.getHealth(), modifiedHealth, mc, left, top, flashAlpha);
		}

		// notify everyone that we should render hunger hud overlay
		HUDOverlayEvent.HungerRestored hungerRenderEvent = new HUDOverlayEvent.HungerRestored(foodLevel, heldItem, modifiedFoodValues, right, top, matrixStack);
		HUDOverlayEvent.HungerRestored.EVENT.invoker().interact(hungerRenderEvent);
		if (hungerRenderEvent.isCanceled)
		{
			resetFlash();
			return;
		}

		// calculate the final hunger and saturation
		int foodHunger = modifiedFoodValues.hunger;
		float foodSaturationIncrement = modifiedFoodValues.getSaturationIncrement();

		// draw hunger overlay
		drawHungerOverlay(hungerRenderEvent, foodHunger, flashAlpha, FoodHelper.isRotten(heldItem), mc);

		int newFoodValue = stats.getFoodLevel() + foodHunger;
		float newSaturationValue = saturationLevel + foodSaturationIncrement;

		// draw saturation overlay of gained
		if (!saturationRenderEvent.isCanceled)
		{
			float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - saturationLevel : foodSaturationIncrement;
			drawSaturationOverlay(saturationRenderEvent, saturationGained, flashAlpha, mc);
		}
	}

	public static void drawSaturationOverlay(MatrixStack matrixStack, float saturationGained, float saturationLevel, MinecraftClient mc, int right, int top, float alpha)
	{
		if (saturationLevel + saturationGained < 0)
			return;

		enableAlpha(alpha);
		mc.getTextureManager().bindTexture(modIcons);

		float modifiedSaturation = Math.min(saturationLevel + saturationGained, 20);

		int startSaturationBar = 0;
		int endSaturationBar = (int)Math.ceil(modifiedSaturation / 2.0F);

		// when require rendering the gained saturation, start should relocation to current saturation tail.
		if (saturationGained != 0)
			startSaturationBar = (int)Math.max(saturationLevel / 2.0F, 0);

		int iconSize = 9;

		for (int i = startSaturationBar; i < endSaturationBar; ++i)
		{
			// gets the offset that needs to be render of icon
			IntPoint offset = foodBarOffsets.get(i);
			if (offset == null)
				continue;

			int x = right + offset.x;
			int y = top + offset.y;

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

	public static void drawHungerOverlay(MatrixStack matrixStack, int hungerRestored, int foodLevel, MinecraftClient mc, int right, int top, float alpha, boolean useRottenTextures)
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
			// gets the offset that needs to be render of icon
			IntPoint offset = foodBarOffsets.get(i);
			if (offset == null)
				continue;

			int x = right + offset.x;
			int y = top + offset.y;

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
			if (i * 2 + 1 == modifiedFood)
				u += 1 * iconSize;

			// very faint background
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha * 0.1F);
			mc.inGameHud.drawTexture(matrixStack, x, y, ub, v, iconSize, iconSize);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);

			mc.inGameHud.drawTexture(matrixStack, x, y, u, v, iconSize, iconSize);
		}

		disableAlpha(alpha);
	}

	public static void drawHealthOverlay(MatrixStack matrixStack, float health, float modifiedHealth, MinecraftClient mc, int right, int top, float alpha)
	{
		if (modifiedHealth <= health)
			return;

		enableAlpha(alpha);
		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);

		int fixedModifiedHealth = (int)Math.ceil(modifiedHealth);
		boolean isHardcore = mc.player.world != null && mc.player.world.getLevelProperties().isHardcore();

		int startHealthBars = (int)(Math.ceil(health) / 2.0F);
		int endHealthBars = (int)Math.ceil(modifiedHealth / 2.0F);

		int iconStartOffset = 16;
		int iconSize = 9;

		for (int i = startHealthBars; i < endHealthBars; ++i)
		{
			// gets the offset that needs to be render of icon
			IntPoint offset = healthBarOffsets.get(i);
			if (offset == null)
				continue;

			int x = right + offset.x;
			int y = top + offset.y;

			// location to full heart icon by default
			int v = 0 * iconSize;
			int u = iconStartOffset + 4 * iconSize;
			int ub = iconStartOffset + 1 * iconSize;

			// relocation to half heart
			if (i * 2 + 1 == fixedModifiedHealth)
				u += 1 * iconSize;

			// relocation to special heart of hardcore
			if (isHardcore)
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

	public static void drawExhaustionOverlay(MatrixStack matrixStack, float exhaustion, MinecraftClient mc, int right, int top, float alpha)
	{
		mc.getTextureManager().bindTexture(modIcons);

		float maxExhaustion = HungerHelper.getMaxExhaustion(mc.player);
		// clamp between 0 and 1
		float ratio = Math.min(1, Math.max(0, exhaustion / maxExhaustion));
		int width = (int) (ratio * 81);
		int height = 9;

		enableAlpha(.75f);
		mc.inGameHud.drawTexture(matrixStack, right - width, top, 81 - width, 18, width, height);
		disableAlpha(.75f);

		// rebind default icons
		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);
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


	private static boolean shouldShowEstimatedHealth(ItemStack hoveredStack, FoodValues modifiedFoodValues)
	{
		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;
		HungerManager stats = player.getHungerManager();

		// in the `PEACEFUL` mode, health will restore faster
		if (player.world.getDifficulty() != Difficulty.PEACEFUL)
			return false;

		// when player has any changes health amount by any case can't show estimated health
		// because player will confused how much of restored/damaged healths
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

	private static void generateBarOffsets(int top, int left, int right, int ticks)
	{
		// hard code in `InGameHUD`
		random.setSeed((long)(ticks * 312871));

		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;
		HungerManager hungerManager = player.getHungerManager();

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
			int y = top - row * heightForHealth;
			// apply the animated offset
			if (shouldAnimatedHealth)
				y += random.nextInt(2);

			// reuse the point object to reduce memory usage
			IntPoint point = healthBarOffsets.get(i);
			if (point == null)
			{
				point = new IntPoint();
				healthBarOffsets.set(i, point);
			}
			point.x = x - left;
			point.y = y - top;
		}

		// when saturation level is zero will random move the food icon
		float saturationLevel = hungerManager.getSaturationLevel();
		int foodLevel = hungerManager.getFoodLevel();
		boolean shouldAnimatedFood = saturationLevel <= 0.0F && ticks % (foodLevel * 3 + 1) == 0;
		for(int i = 0; i < foodBars; ++i)
		{
			int x = right - i * 8 - 9;
			int y = top;

			// apply the animated offset
			if (shouldAnimatedFood)
				y += random.nextInt(3) - 1;

			// reuse the point object to reduce memory usage
			IntPoint point = foodBarOffsets.get(i);
			if (point == null)
			{
				point = new IntPoint();
				foodBarOffsets.set(i, point);
			}

			point.x = x - right;
			point.y = y - top;
		}
	}
}
