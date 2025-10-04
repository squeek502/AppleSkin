package squeek.appleskin.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.world.Difficulty;
import org.jetbrains.annotations.Nullable;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.helpers.*;
import squeek.appleskin.helpers.TextureHelper.FoodType;
import squeek.appleskin.helpers.TextureHelper.HeartType;
import squeek.appleskin.util.IntPoint;

import java.util.Random;
import java.util.Vector;

public class HUDOverlayHandler
{
	public static HUDOverlayHandler INSTANCE;

	private float unclampedFlashAlpha = 0f;
	private float flashAlpha = 0f;
	private byte alphaDir = 1;
	private boolean needDisableBlend = false;

	public final OffsetsCache barOffsets = new OffsetsCache();
	public final HeldFoodCache heldFood = new HeldFoodCache();

	public static void init()
	{
		INSTANCE = new HUDOverlayHandler();
	}

	public void onPreRenderFood(DrawContext context, PlayerEntity player, int top, int right)
	{
		// If ModConfig.INSTANCE is null then we're probably still in the init phase
		if (ModConfig.INSTANCE == null)
			return;

		if (!ModConfig.INSTANCE.showFoodExhaustionHudUnderlay)
			return;

		assert player != null;

		float exhaustion = ExhaustionHelper.getExhaustion(player);

		// Notify everyone that we should render exhaustion hud overlay
		HUDOverlayEvent.Exhaustion renderEvent = new HUDOverlayEvent.Exhaustion(exhaustion, right, top, context);
		HUDOverlayEvent.Exhaustion.EVENT.invoker().interact(renderEvent);
		if (!renderEvent.isCanceled)
		{
			drawExhaustionOverlay(renderEvent, 1f);
		}
	}

	public void onRenderFood(DrawContext context, PlayerEntity player, int top, int right)
	{
		// If ModConfig.INSTANCE is null then we're probably still in the init phase
		if (ModConfig.INSTANCE == null)
			return;

		if (!shouldRenderAnyOverlays())
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		assert player != null;
		HungerManager stats = player.getHungerManager();

		// notify everyone that we should render saturation hud overlay
		HUDOverlayEvent.Saturation saturationRenderEvent = new HUDOverlayEvent.Saturation(stats.getSaturationLevel(), right, top, context);

		// cancel render overlay event when configuration disabled.
		if (!ModConfig.INSTANCE.showSaturationHudOverlay)
			saturationRenderEvent.isCanceled = true;

		// notify everyone that we should render saturation hud overlay
		if (!saturationRenderEvent.isCanceled)
			HUDOverlayEvent.Saturation.EVENT.invoker().interact(saturationRenderEvent);

		// draw saturation overlay
		if (!saturationRenderEvent.isCanceled)
			drawSaturationOverlay(saturationRenderEvent, mc, 0, 1F, mc.inGameHud.getTicks());

		// try to get the item stack in the player hand
		FoodHelper.QueriedFoodResult result = heldFood.result(mc.inGameHud.getTicks(), player);
		if (result == null)
		{
			resetFlash();
			return;
		}

		// restored hunger/saturation overlay while holding food
		if (ModConfig.INSTANCE.showFoodValuesHudOverlay)
		{
			// notify everyone that we should render hunger hud overlay
			HUDOverlayEvent.HungerRestored hungerRenderEvent = new HUDOverlayEvent.HungerRestored(stats.getFoodLevel(), result.itemStack, result.modifiedFoodComponent, right, top, context);
			HUDOverlayEvent.HungerRestored.EVENT.invoker().interact(hungerRenderEvent);
			if (hungerRenderEvent.isCanceled)
				return;

			// calculate the final hunger and saturation
			int foodHunger = result.modifiedFoodComponent.nutrition();
			float foodSaturationIncrement = result.modifiedFoodComponent.saturation();

			// draw hunger overlay
			drawHungerOverlay(hungerRenderEvent, mc, foodHunger, flashAlpha, FoodHelper.isRotten(result.consumableComponent), mc.inGameHud.getTicks());

			int newFoodValue = stats.getFoodLevel() + foodHunger;
			float newSaturationValue = stats.getSaturationLevel() + foodSaturationIncrement;

			// draw saturation overlay of gained
			if (!saturationRenderEvent.isCanceled)
			{
				float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - stats.getSaturationLevel() : foodSaturationIncrement;
				drawSaturationOverlay(saturationRenderEvent, mc, saturationGained, flashAlpha, mc.inGameHud.getTicks());
			}
		}
	}

	public void onRenderHealth(DrawContext context, PlayerEntity player, int left, int top, int lines, int regeneratingHeartIndex, float maxHealth, int lastHealth, int health, int absorption, boolean blinking)
	{
		// If ModConfig.INSTANCE is null then we're probably still in the init phase
		if (ModConfig.INSTANCE == null)
			return;

		if (!shouldRenderAnyOverlays())
			return;

		MinecraftClient mc = MinecraftClient.getInstance();
		assert player != null;

		// try to get the item stack in the player hand
		FoodHelper.QueriedFoodResult result = heldFood.result(mc.inGameHud.getTicks(), player);
		if (result == null)
		{
			resetFlash();
			return;
		}

		// draw health overlay if needed
		if (shouldShowEstimatedHealth(player, mc.inGameHud.getTicks()))
		{
			float foodHealthIncrement = FoodHelper.getEstimatedHealthIncrement(player, new ConsumableFood(result.modifiedFoodComponent, result.consumableComponent));
			float currentHealth = player.getHealth();
			float modifiedHealth = Math.min(currentHealth + foodHealthIncrement, player.getMaxHealth());

			// only create object when the estimated health is successfully
			HUDOverlayEvent.HealthRestored healthRenderEvent = null;
			if (currentHealth < modifiedHealth)
				healthRenderEvent = new HUDOverlayEvent.HealthRestored(modifiedHealth, result.itemStack, result.modifiedFoodComponent, left, top, context);

			// notify everyone that we should render estimated health hud
			if (healthRenderEvent != null)
				HUDOverlayEvent.HealthRestored.EVENT.invoker().interact(healthRenderEvent);

			if (healthRenderEvent != null && !healthRenderEvent.isCanceled)
				drawHealthOverlay(healthRenderEvent, mc, flashAlpha, mc.inGameHud.getTicks());
		}
	}

	public void drawSaturationOverlay(DrawContext context, float saturationGained, float saturationLevel, MinecraftClient mc, int right, int top, float alpha, int guiTicks)
	{
		if (saturationLevel + saturationGained < 0)
			return;

		var alphaColor = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, alpha);

		float modifiedSaturation = Math.max(0, Math.min(saturationLevel + saturationGained, 20));

		int startSaturationBar = 0;
		int endSaturationBar = (int) Math.ceil(modifiedSaturation / 2.0F);

		// when require rendering the gained saturation, start should relocation to current saturation tail.
		if (saturationGained != 0)
			startSaturationBar = (int) Math.max(saturationLevel / 2.0F, 0);

		int iconSize = 9;

		var foodBarOffsets = barOffsets.foodBarOffsets(guiTicks, mc.player);
		for (int i = startSaturationBar; i < endSaturationBar; ++i)
		{
			// gets the offset that needs to be render of icon
			IntPoint offset = i < foodBarOffsets.size() ? foodBarOffsets.get(i) : new IntPoint();
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

			context.drawTexture(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, x, y, u, v, iconSize, iconSize, 256, 256, alphaColor);
		}
	}

	public void drawHungerOverlay(DrawContext context, int hungerRestored, int foodLevel, MinecraftClient mc, int right, int top, float alpha, boolean useRottenTextures, int guiTicks)
	{
		if (hungerRestored <= 0)
			return;

		var alphaColor = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, alpha);

		int modifiedFood = Math.max(0, Math.min(20, foodLevel + hungerRestored));

		int startFoodBars = Math.max(0, foodLevel / 2);
		int endFoodBars = (int) Math.ceil(modifiedFood / 2.0F);

		int iconSize = 9;

		var foodBarOffsets = barOffsets.foodBarOffsets(guiTicks, mc.player);
		for (int i = startFoodBars; i < endFoodBars; ++i)
		{
			// gets the offset that needs to be render of icon
			IntPoint offset = i < foodBarOffsets.size() ? foodBarOffsets.get(i) : new IntPoint();
			if (offset == null)
				continue;

			int x = right + offset.x;
			int y = top + offset.y;

			Identifier backgroundSprite = TextureHelper.getFoodTexture(useRottenTextures, FoodType.EMPTY);

			// very faint background
			var bgColor = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, alpha * 0.25F);
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, backgroundSprite, x, y, iconSize, iconSize, bgColor);

			boolean isHalf = i * 2 + 1 == modifiedFood;
			Identifier iconSprite = TextureHelper.getFoodTexture(useRottenTextures, isHalf ? FoodType.HALF : FoodType.FULL);

			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, iconSprite, x, y, iconSize, iconSize, alphaColor);
		}
	}

	public void drawHealthOverlay(DrawContext context, float health, float modifiedHealth, MinecraftClient mc, int right, int top, float alpha, int guiTicks)
	{
		if (modifiedHealth <= health)
			return;

		var alphaColor = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, alpha);

		int fixedModifiedHealth = (int) Math.ceil(modifiedHealth);
		boolean isHardcore = mc.player.getEntityWorld() != null && mc.player.getEntityWorld().getLevelProperties().isHardcore();

		int startHealthBars = (int) Math.max(0, (Math.ceil(health) / 2.0F));
		int endHealthBars = (int) Math.max(0, Math.ceil(modifiedHealth / 2.0F));

		int iconSize = 9;

		var healthBarOffsets = barOffsets.healthBarOffsets(guiTicks, mc.player);
		for (int i = startHealthBars; i < endHealthBars; ++i)
		{
			// gets the offset that needs to be render of icon
			IntPoint offset = i < healthBarOffsets.size() ? healthBarOffsets.get(i) : new IntPoint();
			if (offset == null)
				continue;

			int x = right + offset.x;
			int y = top + offset.y;

			Identifier backgroundSprite = TextureHelper.getHeartTexture(isHardcore, HeartType.CONTAINER);

			// very faint background
			var bgColor = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, alpha * 0.25F);
			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, backgroundSprite, x, y, iconSize, iconSize, bgColor);

			boolean isHalf = i * 2 + 1 == fixedModifiedHealth;
			Identifier iconSprite = TextureHelper.getHeartTexture(isHardcore, isHalf ? HeartType.HALF : HeartType.FULL);

			context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, iconSprite, x, y, iconSize, iconSize, alphaColor);
		}
	}

	public void drawExhaustionOverlay(DrawContext context, float exhaustion, int right, int top, float alpha)
	{
		float maxExhaustion = FoodHelper.MAX_EXHAUSTION;
		// clamp between 0 and 1
		float ratio = Math.min(1, Math.max(0, exhaustion / maxExhaustion));
		int width = (int) (ratio * 81);
		int height = 9;

		var color = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.75F);
		context.drawTexture(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, right - width, top, 81 - width, 18, width, height, 256, 256, color);
	}


	private void drawSaturationOverlay(HUDOverlayEvent.Saturation event, MinecraftClient mc, float saturationGained, float alpha, int guiTicks)
	{
		drawSaturationOverlay(event.context, saturationGained, event.saturationLevel, mc, event.x, event.y, alpha, guiTicks);
	}

	private void drawHungerOverlay(HUDOverlayEvent.HungerRestored event, MinecraftClient mc, int hunger, float alpha, boolean useRottenTextures, int guiTicks)
	{
		drawHungerOverlay(event.context, hunger, event.currentFoodLevel, mc, event.x, event.y, alpha, useRottenTextures, guiTicks);
	}

	private void drawHealthOverlay(HUDOverlayEvent.HealthRestored event, MinecraftClient mc, float alpha, int guiTicks)
	{
		drawHealthOverlay(event.context, mc.player.getHealth(), event.modifiedHealth, mc, event.x, event.y, alpha, guiTicks);
	}

	private void drawExhaustionOverlay(HUDOverlayEvent.Exhaustion event, float alpha)
	{
		drawExhaustionOverlay(event.context, event.exhaustion, event.x, event.y, alpha);
	}

	private boolean shouldRenderAnyOverlays()
	{
		return ModConfig.INSTANCE.showFoodValuesHudOverlay || ModConfig.INSTANCE.showSaturationHudOverlay || ModConfig.INSTANCE.showFoodHealthHudOverlay;
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


	private boolean shouldShowEstimatedHealth(PlayerEntity player, int guiTicks)
	{
		// then configuration cancel the render event
		if (!ModConfig.INSTANCE.showFoodHealthHudOverlay)
			return false;

		// Offsets size is set to zero intentionally to disable rendering when health is infinite.
		if (barOffsets.healthBarOffsets(guiTicks, player).isEmpty())
			return false;

		HungerManager stats = player.getHungerManager();

		// in the `PEACEFUL` mode, health will restore faster
		if (player.getEntityWorld().getDifficulty() == Difficulty.PEACEFUL)
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

	private static class OffsetsCache
	{
		protected final Vector<IntPoint> foodBarOffsets = new Vector<>();
		protected final Vector<IntPoint> healthBarOffsets = new Vector<>();
		public int lastGuiTick = 0;
		protected final Random random = new Random();

		protected void generate(int guiTicks, PlayerEntity player)
		{
			// Note: Both health and food offsets are generated together
			// because the PRNG used for the health icon offsets affects
			// the PRNG of the food icon offsets. By generating the offsets
			// together, we can match the PRNG of the Vanilla HUD.

			final int preferHealthBars = 10;
			final int preferFoodBars = 10;

			final float maxHealth = player.getMaxHealth();
			final float absorptionHealth = (float) Math.ceil(player.getAbsorptionAmount());

			int healthBars = (int) Math.ceil((maxHealth + absorptionHealth) / 2.0F);
			// When maxHealth + absorptionHealth is greater than Integer.INT_MAX,
			// Minecraft will disable heart rendering due to a quirk of MathHelper.ceil.
			// We have a much lower threshold since there's no reason to get the offsets
			// for thousands of hearts.
			// Note: Infinite and > INT_MAX absorption has been seen in the wild.
			// This will effectively disable rendering whenever health is unexpectedly large.
			if (healthBars < 0 || healthBars > 1000)
			{
				healthBars = 0;
			}

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
				shouldAnimatedFood = saturationLevel <= 0.0F && guiTicks % (foodLevel * 3 + 1) == 0;

				// in vanilla health is too low (below 5) will show heartbeat animation
				// when regeneration will also show heartbeat animation, but we don't need now
				shouldAnimatedHealth = Math.ceil(player.getHealth()) <= 4;
			}

			// hard code in `InGameHUD`
			random.setSeed((long) (guiTicks * 312871));

			// adjust the size
			if (healthBarOffsets.size() != healthBars)
				healthBarOffsets.setSize(healthBars);

			if (foodBarOffsets.size() != preferFoodBars)
				foodBarOffsets.setSize(preferFoodBars);

			// left alignment, multiple rows, reverse
			for (int i = healthBars - 1; i >= 0; --i)
			{
				int row = (int) Math.ceil((float) (i + 1) / (float) preferHealthBars) - 1;
				int x = i % preferHealthBars * 8;
				int y = -(row * healthRowHeight);
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

				point.x = x;
				point.y = y;
			}

			// right alignment, single row
			for (int i = 0; i < preferFoodBars; ++i)
			{
				int x = -(i * 8) - 9;
				int y = 0;

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

				point.x = x;
				point.y = y;
			}
		}

		public Vector<IntPoint> healthBarOffsets(int guiTick, PlayerEntity player)
		{
			if (guiTick != lastGuiTick)
			{
				generate(guiTick, player);
				lastGuiTick = guiTick;
			}
			return this.healthBarOffsets;
		}

		public Vector<IntPoint> foodBarOffsets(int guiTicks, PlayerEntity player)
		{
			if (guiTicks != lastGuiTick)
			{
				generate(guiTicks, player);
				lastGuiTick = guiTicks;
			}
			return this.foodBarOffsets;
		}
	}

	public static class HeldFoodCache
	{
		@Nullable
		protected FoodHelper.QueriedFoodResult result;
		public int lastGuiTick = 0;

		protected void query(PlayerEntity player)
		{
			// try to get the item stack in the player hand
			ItemStack heldItem = player.getMainHandStack();
			FoodHelper.QueriedFoodResult heldFood = FoodHelper.query(heldItem, player);
			boolean canConsume = heldFood != null && FoodHelper.canConsume(player, heldFood.modifiedFoodComponent);
			if (ModConfig.INSTANCE.showFoodValuesHudOverlayWhenOffhand && !canConsume)
			{
				heldItem = player.getOffHandStack();
				heldFood = FoodHelper.query(heldItem, player);
				canConsume = heldFood != null && FoodHelper.canConsume(player, heldFood.modifiedFoodComponent);
			}

			boolean shouldRenderHeldItemValues = !heldItem.isEmpty() && canConsume;
			if (!shouldRenderHeldItemValues)
			{
				this.result = null;
				return;
			}

			this.result = heldFood;
		}

		public FoodHelper.QueriedFoodResult result(int guiTick, PlayerEntity player)
		{
			if (guiTick != lastGuiTick)
			{
				query(player);
				lastGuiTick = guiTick;
			}
			return this.result;
		}
	}
}
