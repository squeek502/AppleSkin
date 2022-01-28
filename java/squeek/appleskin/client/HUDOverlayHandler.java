package squeek.appleskin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
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
import squeek.appleskin.util.IntPoint;

import java.util.Random;
import java.util.Vector;

public class HUDOverlayHandler
{
	public static HUDOverlayHandler INSTANCE;

	private float unclampedFlashAlpha = 0f;
	private float flashAlpha = 0f;
	private byte alphaDir = 1;
	private int foodIconsOffset;

	public int FOOD_BAR_HEIGHT = 39;

	public final Vector<IntPoint> healthBarOffsets = new Vector<>();
	public final Vector<IntPoint> foodBarOffsets = new Vector<>();

	private final Random random = new Random();
	private final Identifier modIcons = new Identifier("appleskin", "textures/icons.png");

	public static void init()
	{
		INSTANCE = new HUDOverlayHandler();
	}

	public void onPreRender(MatrixStack matrixStack)
	{
		foodIconsOffset = FOOD_BAR_HEIGHT;

		// If ModConfig.INSTANCE is null then we're probably still in the init phase
		if (ModConfig.INSTANCE == null)
			return;

		if (!ModConfig.INSTANCE.showFoodExhaustionHudUnderlay)
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;
		assert player != null;

		int right = mc.getWindow().getScaledWidth() / 2 + 91;
		int top = mc.getWindow().getScaledHeight() - foodIconsOffset;
		float exhaustion = player.getHungerManager().getExhaustion();

		// Notify everyone that we should render exhaustion hud overlay
		HUDOverlayEvent.Exhaustion renderEvent = new HUDOverlayEvent.Exhaustion(exhaustion, right, top, matrixStack);
		HUDOverlayEvent.Exhaustion.EVENT.invoker().interact(renderEvent);
		if (!renderEvent.isCanceled)
		{
			drawExhaustionOverlay(renderEvent, mc, 1f);
		}
	}

	public void onRender(MatrixStack matrixStack)
	{
		// If ModConfig.INSTANCE is null then we're probably still in the init phase
		if (ModConfig.INSTANCE == null)
			return;

		if (!shouldRenderAnyOverlays())
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;
		assert player != null;
		HungerManager stats = player.getHungerManager();

		int top = mc.getWindow().getScaledHeight() - foodIconsOffset;
		int left = mc.getWindow().getScaledWidth() / 2 - 91; // left of health bar
		int right = mc.getWindow().getScaledWidth() / 2 + 91; // right of food bar

		// generate at the beginning to avoid ArrayIndexOutOfBoundsException
		generateBarOffsets(top, left, right, mc.inGameHud.getTicks(), player);

		// notify everyone that we should render saturation hud overlay
		HUDOverlayEvent.Saturation saturationRenderEvent = new HUDOverlayEvent.Saturation(stats.getSaturationLevel(), right, top, matrixStack);

		// cancel render overlay event when configuration disabled.
		if (!ModConfig.INSTANCE.showSaturationHudOverlay)
			saturationRenderEvent.isCanceled = true;

		// notify everyone that we should render saturation hud overlay
		if (!saturationRenderEvent.isCanceled)
			HUDOverlayEvent.Saturation.EVENT.invoker().interact(saturationRenderEvent);

		// draw saturation overlay
		if (!saturationRenderEvent.isCanceled)
			drawSaturationOverlay(saturationRenderEvent, mc, 0, 1F);

		// try to get the item stack in the player hand
		ItemStack heldItem = player.getMainHandStack();
		if (ModConfig.INSTANCE.showFoodValuesHudOverlayWhenOffhand && !FoodHelper.canConsume(heldItem, player))
			heldItem = player.getOffHandStack();

		boolean shouldRenderHeldItemValues = !heldItem.isEmpty() && FoodHelper.canConsume(heldItem, player);
		if (!shouldRenderHeldItemValues)
		{
			resetFlash();
			return;
		}

		// restored hunger/saturation overlay while holding food
		FoodValues modifiedFoodValues = FoodHelper.getModifiedFoodValues(heldItem, player);
		FoodValuesEvent foodValuesEvent = new FoodValuesEvent(player, heldItem, FoodHelper.getDefaultFoodValues(heldItem), modifiedFoodValues);
		FoodValuesEvent.EVENT.invoker().interact(foodValuesEvent);
		modifiedFoodValues = foodValuesEvent.modifiedFoodValues;

		// draw health overlay if needed
		if (shouldShowEstimatedHealth(heldItem, modifiedFoodValues))
		{
			float foodHealthIncrement = FoodHelper.getEstimatedHealthIncrement(heldItem, modifiedFoodValues, player);
			float currentHealth = player.getHealth();
			float modifiedHealth = Math.min(currentHealth + foodHealthIncrement, player.getMaxHealth());

			// only create object when the estimated health is successfully
			HUDOverlayEvent.HealthRestored healthRenderEvent = null;
			if (currentHealth < modifiedHealth)
				healthRenderEvent = new HUDOverlayEvent.HealthRestored(modifiedHealth, heldItem, modifiedFoodValues, left, top, matrixStack);

			// notify everyone that we should render estimated health hud
			if (healthRenderEvent != null)
				HUDOverlayEvent.HealthRestored.EVENT.invoker().interact(healthRenderEvent);

			if (healthRenderEvent != null && !healthRenderEvent.isCanceled)
				drawHealthOverlay(healthRenderEvent, mc, flashAlpha);
		}

		if (ModConfig.INSTANCE.showFoodValuesHudOverlay)
		{
			// notify everyone that we should render hunger hud overlay
			HUDOverlayEvent.HungerRestored hungerRenderEvent = new HUDOverlayEvent.HungerRestored(stats.getFoodLevel(), heldItem, modifiedFoodValues, right, top, matrixStack);
			HUDOverlayEvent.HungerRestored.EVENT.invoker().interact(hungerRenderEvent);
			if (hungerRenderEvent.isCanceled)
				return;

			// calculate the final hunger and saturation
			int foodHunger = modifiedFoodValues.hunger;
			float foodSaturationIncrement = modifiedFoodValues.getSaturationIncrement();

			// draw hunger overlay
			drawHungerOverlay(hungerRenderEvent, mc, foodHunger, flashAlpha, FoodHelper.isRotten(heldItem));

			int newFoodValue = stats.getFoodLevel() + foodHunger;
			float newSaturationValue = stats.getSaturationLevel() + foodSaturationIncrement;

			// draw saturation overlay of gained
			if (!saturationRenderEvent.isCanceled)
			{
				float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - stats.getSaturationLevel() : foodSaturationIncrement;
				drawSaturationOverlay(saturationRenderEvent, mc, saturationGained, flashAlpha);
			}
		}
	}

	public void drawSaturationOverlay(MatrixStack matrixStack, float saturationGained, float saturationLevel, MinecraftClient mc, int right, int top, float alpha)
	{
		if (saturationLevel + saturationGained < 0)
			return;

		enableAlpha(alpha);
		RenderSystem.setShaderTexture(0, modIcons);

		float modifiedSaturation = Math.max(0, Math.min(saturationLevel + saturationGained, 20));

		int startSaturationBar = 0;
		int endSaturationBar = (int) Math.ceil(modifiedSaturation / 2.0F);

		// when require rendering the gained saturation, start should relocation to current saturation tail.
		if (saturationGained != 0)
			startSaturationBar = (int) Math.max(saturationLevel / 2.0F, 0);

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
		RenderSystem.setShaderTexture(0, Screen.GUI_ICONS_TEXTURE);
		disableAlpha(alpha);
	}

	public void drawHungerOverlay(MatrixStack matrixStack, int hungerRestored, int foodLevel, MinecraftClient mc, int right, int top, float alpha, boolean useRottenTextures)
	{
		if (hungerRestored <= 0)
			return;

		enableAlpha(alpha);
		RenderSystem.setShaderTexture(0, Screen.GUI_ICONS_TEXTURE);

		int modifiedFood = Math.max(0, Math.min(20, foodLevel + hungerRestored));

		int startFoodBars = Math.max(0, foodLevel / 2);
		int endFoodBars = (int) Math.ceil(modifiedFood / 2.0F);

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
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * 0.25F);
			mc.inGameHud.drawTexture(matrixStack, x, y, ub, v, iconSize, iconSize);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

			mc.inGameHud.drawTexture(matrixStack, x, y, u, v, iconSize, iconSize);
		}

		disableAlpha(alpha);
	}

	public void drawHealthOverlay(MatrixStack matrixStack, float health, float modifiedHealth, MinecraftClient mc, int right, int top, float alpha)
	{
		if (modifiedHealth <= health)
			return;

		enableAlpha(alpha);
		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);

		int fixedModifiedHealth = (int) Math.ceil(modifiedHealth);
		boolean isHardcore = mc.player.world != null && mc.player.world.getLevelProperties().isHardcore();

		int startHealthBars = (int) Math.max(0, (Math.ceil(health) / 2.0F));
		int endHealthBars = (int) Math.max(0, Math.ceil(modifiedHealth / 2.0F));

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
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha * 0.25F);
			mc.inGameHud.drawTexture(matrixStack, x, y, ub, v, iconSize, iconSize);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

			mc.inGameHud.drawTexture(matrixStack, x, y, u, v, iconSize, iconSize);
		}

		disableAlpha(alpha);
	}

	public void drawExhaustionOverlay(MatrixStack matrixStack, float exhaustion, MinecraftClient mc, int right, int top, float alpha)
	{
		RenderSystem.setShaderTexture(0, modIcons);

		float maxExhaustion = FoodHelper.MAX_EXHAUSTION;
		// clamp between 0 and 1
		float ratio = Math.min(1, Math.max(0, exhaustion / maxExhaustion));
		int width = (int) (ratio * 81);
		int height = 9;

		enableAlpha(.75f);
		mc.inGameHud.drawTexture(matrixStack, right - width, top, 81 - width, 18, width, height);
		disableAlpha(.75f);

		// rebind default icons
		RenderSystem.setShaderTexture(0, Screen.GUI_ICONS_TEXTURE);
	}


	private void drawSaturationOverlay(HUDOverlayEvent.Saturation event, MinecraftClient mc, float saturationGained, float alpha)
	{
		drawSaturationOverlay(event.matrixStack, saturationGained, event.saturationLevel, mc, event.x, event.y, alpha);
	}

	private void drawHungerOverlay(HUDOverlayEvent.HungerRestored event, MinecraftClient mc, int hunger, float alpha, boolean useRottenTextures)
	{
		drawHungerOverlay(event.matrixStack, hunger, event.currentFoodLevel, mc, event.x, event.y, alpha, useRottenTextures);
	}

	private void drawHealthOverlay(HUDOverlayEvent.HealthRestored event, MinecraftClient mc, float alpha)
	{
		drawHealthOverlay(event.matrixStack, mc.player.getHealth(), event.modifiedHealth, mc, event.x, event.y, alpha);
	}

	private void drawExhaustionOverlay(HUDOverlayEvent.Exhaustion event, MinecraftClient mc, float alpha)
	{
		drawExhaustionOverlay(event.matrixStack, event.exhaustion, mc, event.x, event.y, alpha);
	}

	private boolean shouldRenderAnyOverlays()
	{
		return ModConfig.INSTANCE.showFoodValuesHudOverlay || ModConfig.INSTANCE.showSaturationHudOverlay || ModConfig.INSTANCE.showFoodHealthHudOverlay;
	}

	private void enableAlpha(float alpha)
	{
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
		RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
	}

	private void disableAlpha(float alpha)
	{
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}


	public void onClientTick()
	{
		unclampedFlashAlpha += alphaDir * 0.125F;
		if (unclampedFlashAlpha >= 1.5F)
		{
			alphaDir = -1;
		}
		else if (unclampedFlashAlpha <= -0.5F)
		{
			alphaDir = 1;
		}
		flashAlpha = Math.max(0F, Math.min(1F, unclampedFlashAlpha)) * Math.max(0F, Math.min(1F, ModConfig.INSTANCE.maxHudOverlayFlashAlpha));
	}

	public void resetFlash()
	{
		unclampedFlashAlpha = flashAlpha = 0;
		alphaDir = 1;
	}


	private boolean shouldShowEstimatedHealth(ItemStack hoveredStack, FoodValues modifiedFoodValues)
	{
		// then configuration cancel the render event
		if (!ModConfig.INSTANCE.showFoodHealthHudOverlay)
			return false;

		// Offsets size is set to zero intentionally to disable rendering when health is infinite.
		if (healthBarOffsets.size() == 0)
			return false;

		MinecraftClient mc = MinecraftClient.getInstance();
		PlayerEntity player = mc.player;
		HungerManager stats = player.getHungerManager();

		// in the `PEACEFUL` mode, health will restore faster
		if (player.world.getDifficulty() == Difficulty.PEACEFUL)
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

	private void generateBarOffsets(int top, int left, int right, int ticks, PlayerEntity player)
	{
		final int preferHealthBars = 10;
		final int preferFoodBars = 10;

		final float maxHealth = player.getMaxHealth();
		final float absorptionHealth = (float) Math.ceil(player.getAbsorptionAmount());

		int healthBars = (int) Math.ceil((maxHealth + absorptionHealth) / 2.0F);
		int healthRows = (int) Math.ceil((float) healthBars / (float) preferHealthBars);

		int healthRowHeight = Math.max(10 - (healthRows - 2), 3);

		boolean shouldAnimatedHealth = false;
		boolean shouldAnimatedFood = false;

		// when some mods using custom render, we need to least provide an option to cancel animation
		if (ModConfig.INSTANCE.showVanillaAnimationsOverlay)
		{
			HungerManager hungerManager = player.getHungerManager();

			// in vanilla saturation level is zero will show hunger animation
			float saturationLevel = hungerManager.getSaturationLevel();
			int foodLevel = hungerManager.getFoodLevel();
			shouldAnimatedFood = saturationLevel <= 0.0F && ticks % (foodLevel * 3 + 1) == 0;

			// in vanilla health is too low (below 5) will show heartbeat animation
			// when regeneration will also show heartbeat animation, but we don't need now
			shouldAnimatedHealth = Math.ceil(player.getHealth()) <= 4;
		}

		// hard code in `InGameHUD`
		random.setSeed((long) (ticks * 312871));

		// Special case for infinite/NaN. Infinite absorption has been seen in the wild.
		// This will effectively disable rendering while health is infinite.
		if (!Float.isFinite(maxHealth + absorptionHealth))
			healthBars = 0;

		// adjust the size
		if (healthBarOffsets.size() != healthBars)
			healthBarOffsets.setSize(healthBars);

		if (foodBarOffsets.size() != preferFoodBars)
			foodBarOffsets.setSize(preferFoodBars);

		// left alignment, multiple rows, reverse
		for (int i = healthBars - 1; i >= 0; --i)
		{
			int row = (int) Math.ceil((float) (i + 1) / (float) preferHealthBars) - 1;
			int x = left + i % preferHealthBars * 8;
			int y = top - row * healthRowHeight;
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

		// right alignment, single row
		for (int i = 0; i < preferFoodBars; ++i)
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
