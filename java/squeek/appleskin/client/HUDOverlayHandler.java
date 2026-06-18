package squeek.appleskin.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.GuiLayer;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import squeek.appleskin.ModConfig;
import squeek.appleskin.ModInfo;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.helpers.*;
import squeek.appleskin.util.IntPoint;

import java.util.Vector;

public class HUDOverlayHandler
{
	private static float unclampedFlashAlpha = 0f;
	private static float flashAlpha = 0f;
	private static byte alphaDir = 1;
	protected static int foodIconsOffset;
	protected static int healthIconsOffset;

	private static final OffsetsCache barOffsets = new OffsetsCache();
	private static final HeldFoodCache heldFood = new HeldFoodCache();

	private static final RandomSource random = RandomSource.create();

	public static void register(RegisterGuiLayersEvent event)
	{
		// register dummy layers that just store the gui.leftHeight/gui.rightHeight
		// before they get modified during the rendering of the health/food HUD
		event.registerBelow(
			VanillaGuiLayers.PLAYER_HEALTH,
			Identifier.fromNamespaceAndPath(ModInfo.MODID, "health_offset"),
			(guiGraphics, deltaTracker) -> healthIconsOffset = Minecraft.getInstance().gui.hud.leftHeight
		);
		event.registerBelow(
			VanillaGuiLayers.FOOD_LEVEL,
			Identifier.fromNamespaceAndPath(ModInfo.MODID, "food_offset"),
			(guiGraphics, deltaTracker) -> foodIconsOffset = Minecraft.getInstance().gui.hud.rightHeight
		);

		// register overlays/underlays.
		event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH, HealthOverlay.ID, new HealthOverlay());
		event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, HungerOverlay.ID, new HungerOverlay());
		event.registerAbove(VanillaGuiLayers.FOOD_LEVEL, SaturationOverlay.ID, new SaturationOverlay());
		event.registerBelow(VanillaGuiLayers.FOOD_LEVEL, ExhaustionOverlay.ID, new ExhaustionOverlay());

		// register tick callback.
		NeoForge.EVENT_BUS.addListener(HUDOverlayHandler::onClientTick);
	}

	public static abstract class Overlay implements GuiLayer
	{
		public abstract void render(Minecraft mc, Player player, GuiGraphicsExtractor guiGraphics, int left, int right, int top, int guiTicks);

		@Override
		public final void render(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker)
		{
			Minecraft mc = Minecraft.getInstance();
			if (mc.player == null || !shouldRenderOverlay(mc, mc.player, guiGraphics, mc.gui.hud.getGuiTicks()))
				return;

			int top = guiGraphics.guiHeight();
			int left = guiGraphics.guiWidth() / 2 - 91; // left of health bar
			int right = guiGraphics.guiWidth() / 2 + 91; // right of food bar

			render(mc, mc.player, guiGraphics, left, right, top, mc.gui.hud.getGuiTicks());
		}

		public boolean shouldRenderOverlay(Minecraft mc, Player player, GuiGraphicsExtractor guiGraphics, int guiTicks)
		{
			return !mc.gui.hud.isHidden() && mc.gameMode != null && mc.gameMode.canHurtPlayer();
		}
	}

	// TODO: missing healthBlinkTime, see net.minecraft.client.gui.Gui#renderHealthLevel
	public static class HealthOverlay extends Overlay
	{
		public static final Identifier ID = Identifier.fromNamespaceAndPath(ModInfo.MODID, "health_restored");

		@Override
		public void render(Minecraft mc, Player player, GuiGraphicsExtractor guiGraphics, int left, int right, int top, int guiTicks)
		{
			FoodHelper.QueriedFoodResult result = heldFood.result(guiTicks, player);
			if (result == null)
			{
				resetFlash();
				return;
			}

			float foodHealthIncrement = FoodHelper.getEstimatedHealthIncrement(player, new ConsumableFood(result.modifiedFoodProperties, result.consumable));
			float currentHealth = player.getHealth();
			float modifiedHealth = Math.min(currentHealth + foodHealthIncrement, player.getMaxHealth());

			// only create object when the estimated health is successfully
			HUDOverlayEvent.HealthRestored healthRenderEvent = null;
			if (currentHealth < modifiedHealth)
				healthRenderEvent = new HUDOverlayEvent.HealthRestored(modifiedHealth, result.itemStack, result.modifiedFoodProperties, left, top - healthIconsOffset, guiGraphics);

			// notify everyone that we should render estimated health hud
			if (healthRenderEvent != null)
				NeoForge.EVENT_BUS.post(healthRenderEvent);

			if (healthRenderEvent != null && !healthRenderEvent.isCanceled())
				drawHealthOverlay(healthRenderEvent, player, flashAlpha, guiTicks);
		}

		@Override
		public boolean shouldRenderOverlay(Minecraft mc, Player player, GuiGraphicsExtractor guiGraphics, int guiTicks)
		{
			if (!super.shouldRenderOverlay(mc, player, guiGraphics, guiTicks))
				return false;

			if (isMountHealthShown(player))
				return false;

			// Offsets size is set to zero intentionally to disable rendering when health is infinite.
			if (barOffsets.healthBarOffsets(guiTicks, player).isEmpty())
				return false;

			return shouldShowEstimatedHealth(player);
		}
	}

	public static class HungerOverlay extends Overlay
	{
		public static final Identifier ID = Identifier.fromNamespaceAndPath(ModInfo.MODID, "hunger_restored");

		@Override
		public void render(Minecraft mc, Player player, GuiGraphicsExtractor guiGraphics, int left, int right, int top, int guiTicks)
		{
			FoodData stats = player.getFoodData();
			FoodHelper.QueriedFoodResult result = heldFood.result(guiTicks, player);
			if (result == null)
			{
				resetFlash();
				return;
			}

			ItemStack heldItem = result.itemStack;
			FoodProperties modifiedFoodProperties = result.modifiedFoodProperties;

			// notify everyone that we should render hunger hud overlay
			HUDOverlayEvent.HungerRestored renderRenderEvent = new HUDOverlayEvent.HungerRestored(stats.getFoodLevel(), heldItem, modifiedFoodProperties, right, top - foodIconsOffset, guiGraphics);
			NeoForge.EVENT_BUS.post(renderRenderEvent);
			if (renderRenderEvent.isCanceled())
				return;

			// calculate the final hunger and saturation
			int foodHunger = modifiedFoodProperties.nutrition();
			float foodSaturationIncrement = modifiedFoodProperties.saturation();

			// restored hunger/saturation overlay while holding food
			drawHungerOverlay(renderRenderEvent, player, foodHunger, flashAlpha, FoodHelper.isRotten(result.consumable), guiTicks);
		}

		@Override
		public boolean shouldRenderOverlay(Minecraft mc, Player player, GuiGraphicsExtractor guiGraphics, int guiTicks)
		{
			if (!super.shouldRenderOverlay(mc, player, guiGraphics, guiTicks))
				return false;

			if (isMountHealthShown(player))
				return false;

			return ModConfig.SHOW_FOOD_VALUES_OVERLAY.get();
		}
	}

	public static class SaturationOverlay extends Overlay
	{
		public static final Identifier ID = Identifier.fromNamespaceAndPath(ModInfo.MODID, "saturation_level");

		@Override
		public void render(Minecraft mc, Player player, GuiGraphicsExtractor guiGraphics, int left, int right, int top, int guiTicks)
		{
			FoodData stats = player.getFoodData();
			HUDOverlayEvent.Saturation saturationRenderEvent = new HUDOverlayEvent.Saturation(stats.getSaturationLevel(), right, top - foodIconsOffset, guiGraphics);

			// notify everyone that we should render saturation hud overlay
			if (!saturationRenderEvent.isCanceled())
				NeoForge.EVENT_BUS.post(saturationRenderEvent);

			// the render saturation event maybe cancelled by other mods
			if (saturationRenderEvent.isCanceled())
				return;

			drawSaturationOverlay(saturationRenderEvent, player, 0, 1f, guiTicks);

			if (!ModConfig.SHOW_FOOD_VALUES_OVERLAY.get())
				return;

			FoodHelper.QueriedFoodResult result = heldFood.result(guiTicks, player);
			if (result == null)
				return;

			// calculate the final hunger and saturation
			FoodProperties modifiedFoodProperties = result.modifiedFoodProperties;
			int foodHunger = modifiedFoodProperties.nutrition();
			float foodSaturationIncrement = modifiedFoodProperties.saturation();

			int newFoodValue = stats.getFoodLevel() + foodHunger;
			float newSaturationValue = stats.getSaturationLevel() + foodSaturationIncrement;
			float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - stats.getSaturationLevel() : foodSaturationIncrement;
			// Redraw saturation overlay for gained
			drawSaturationOverlay(saturationRenderEvent, player, saturationGained, flashAlpha, guiTicks);
		}

		@Override
		public boolean shouldRenderOverlay(Minecraft mc, Player player, GuiGraphicsExtractor guiGraphics, int guiTicks)
		{
			if (!super.shouldRenderOverlay(mc, player, guiGraphics, guiTicks))
				return false;

			if (isMountHealthShown(player))
				return false;

			return ModConfig.SHOW_SATURATION_OVERLAY.get();
		}
	}

	public static class ExhaustionOverlay extends Overlay
	{
		public static final Identifier ID = Identifier.fromNamespaceAndPath(ModInfo.MODID, "exhaustion_level");

		@Override
		public void render(Minecraft mc, Player player, GuiGraphicsExtractor guiGraphics, int left, int right, int top, int guiTicks)
		{
			float exhaustion = player.getFoodData().exhaustionLevel;

			// Notify everyone that we should render exhaustion hud overlay
			HUDOverlayEvent.Exhaustion renderEvent = new HUDOverlayEvent.Exhaustion(exhaustion, right, top - foodIconsOffset, guiGraphics);
			NeoForge.EVENT_BUS.post(renderEvent);
			if (!renderEvent.isCanceled())
				drawExhaustionOverlay(renderEvent, player, 1f);
		}

		@Override
		public boolean shouldRenderOverlay(Minecraft mc, Player player, GuiGraphicsExtractor guiGraphics, int guiTicks)
		{
			if (!super.shouldRenderOverlay(mc, player, guiGraphics, guiTicks))
				return false;

			if (isMountHealthShown(player))
				return false;

			return ModConfig.SHOW_FOOD_EXHAUSTION_UNDERLAY.get();
		}
	}

	public static boolean isMountHealthShown(Player player)
	{
		return player.getVehicle() != null && player.getVehicle().showVehicleHealth();
	}

	public static void drawSaturationOverlay(float saturationGained, float saturationLevel, Player player, GuiGraphicsExtractor guiGraphics, int right, int top, float alpha, int guiTicks)
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

		var offsets = barOffsets.foodBarOffsets(guiTicks, player);
		for (int i = startSaturationBar; i < endSaturationBar; ++i)
		{
			// gets the offset that needs to be render of icon
			IntPoint offset = i < offsets.size() ? offsets.get(i) : new IntPoint();
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

			guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, x, y, u, v, iconSize, iconSize, 256, 256, alphaColor);
		}
	}

	public static void drawHungerOverlay(int hungerRestored, int foodLevel, Player player, GuiGraphicsExtractor guiGraphics, int right, int top, float alpha, boolean useRottenTextures, int guiTicks)
	{
		if (hungerRestored <= 0)
			return;

		var alphaColor = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, alpha);

		int modifiedFood = Math.max(0, Math.min(20, foodLevel + hungerRestored));

		int startFoodBars = Math.max(0, foodLevel / 2);
		int endFoodBars = (int) Math.ceil(modifiedFood / 2.0F);

		int iconStartOffset = 16;
		int iconSize = 9;

		var offsets = barOffsets.foodBarOffsets(guiTicks, player);
		for (int i = startFoodBars; i < endFoodBars; ++i)
		{
			// gets the offset that needs to be render of icon
			IntPoint offset = i < offsets.size() ? offsets.get(i) : new IntPoint();
			if (offset == null)
				continue;

			int x = right + offset.x;
			int y = top + offset.y;

			Identifier backgroundSprite = TextureHelper.getFoodTexture(useRottenTextures, TextureHelper.FoodType.EMPTY);

			// very faint background
			var bgColor = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, alpha * 0.25F);
			guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, backgroundSprite, x, y, iconSize, iconSize, bgColor);

			boolean isHalf = i * 2 + 1 == modifiedFood;
			Identifier iconSprite = TextureHelper.getFoodTexture(useRottenTextures, isHalf ? TextureHelper.FoodType.HALF : TextureHelper.FoodType.FULL);

			guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconSprite, x, y, iconSize, iconSize, alphaColor);
		}
	}

	public static void drawHealthOverlay(float health, float modifiedHealth, Player player, GuiGraphicsExtractor guiGraphics, int right, int top, float alpha, int guiTicks)
	{
		if (modifiedHealth <= health)
			return;

		var alphaColor = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, alpha);

		int fixedModifiedHealth = (int) Math.ceil(modifiedHealth);
		boolean isHardcore = player.level().getLevelData().isHardcore();

		int startHealthBars = (int) Math.max(0, (Math.ceil(health) / 2.0F));
		int endHealthBars = (int) Math.max(0, Math.ceil(modifiedHealth / 2.0F));

		int iconSize = 9;

		var offsets = barOffsets.healthBarOffsets(guiTicks, player);
		for (int i = startHealthBars; i < endHealthBars; ++i)
		{
			// gets the offset that needs to be render of icon
			IntPoint offset = i < offsets.size() ? offsets.get(i) : new IntPoint();
			if (offset == null)
				continue;

			int x = right + offset.x;
			int y = top + offset.y;

			Identifier backgroundSprite = TextureHelper.getHeartTexture(isHardcore, TextureHelper.HeartType.CONTAINER);

			// very faint background
			var bgColor = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, alpha * 0.25F);
			guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, backgroundSprite, x, y, iconSize, iconSize, bgColor);

			boolean isHalf = i * 2 + 1 == fixedModifiedHealth;
			Identifier iconSprite = TextureHelper.getHeartTexture(isHardcore, isHalf ? TextureHelper.HeartType.HALF : TextureHelper.HeartType.FULL);

			guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, iconSprite, x, y, iconSize, iconSize, alphaColor);
		}
	}

	public static void drawExhaustionOverlay(float exhaustion, Player player, GuiGraphicsExtractor guiGraphics, int right, int top, float alpha)
	{
		float maxExhaustion = HungerHelper.getMaxExhaustion(player);
		// clamp between 0 and 1
		float ratio = Math.min(1, Math.max(0, exhaustion / maxExhaustion));
		int width = (int) (ratio * 81);
		int height = 9;

		var color = ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.75F);
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, right - width, top, 81 - width, 18, width, height, 256, 256, color);
	}

	public static void onClientTick(ClientTickEvent.Post event)
	{
		unclampedFlashAlpha += alphaDir * 0.125f;
		if (unclampedFlashAlpha >= 1.5f)
		{
			alphaDir = -1;
		}
		else if (unclampedFlashAlpha <= -0.5f)
		{
			alphaDir = 1;
		}
		flashAlpha = Math.max(0F, Math.min(1F, unclampedFlashAlpha)) * Math.max(0F, Math.min(1F, ModConfig.MAX_HUD_OVERLAY_FLASH_ALPHA.get().floatValue()));
	}

	public static void resetFlash()
	{
		unclampedFlashAlpha = flashAlpha = 0f;
		alphaDir = 1;
	}

	private static void drawSaturationOverlay(HUDOverlayEvent.Saturation event, Player player, float saturationGained, float alpha, int guiTicks)
	{
		drawSaturationOverlay(saturationGained, event.saturationLevel, player, event.guiGraphics, event.x, event.y, alpha, guiTicks);
	}

	private static void drawHungerOverlay(HUDOverlayEvent.HungerRestored event, Player player, int hunger, float alpha, boolean useRottenTextures, int guiTicks)
	{
		drawHungerOverlay(hunger, event.currentFoodLevel, player, event.guiGraphics, event.x, event.y, alpha, useRottenTextures, guiTicks);
	}

	private static void drawHealthOverlay(HUDOverlayEvent.HealthRestored event, Player player, float alpha, int guiTicks)
	{
		drawHealthOverlay(player.getHealth(), event.modifiedHealth, player, event.guiGraphics, event.x, event.y, alpha, guiTicks);
	}

	private static void drawExhaustionOverlay(HUDOverlayEvent.Exhaustion event, Player player, float alpha)
	{
		drawExhaustionOverlay(event.exhaustion, player, event.guiGraphics, event.x, event.y, alpha);
	}

	private static boolean shouldShowEstimatedHealth(Player player)
	{
		// then configuration cancel the render event
		if (!ModConfig.SHOW_FOOD_HEALTH_HUD_OVERLAY.get())
			return false;

		FoodData stats = player.getFoodData();

		// in the `PEACEFUL` mode, health will restore faster
		if (player.level().getDifficulty() == Difficulty.PEACEFUL)
			return false;

		// when player has any changes health amount by any case can't show estimated health
		// because player will confused how much of restored/damaged healths
		if (stats.getFoodLevel() >= 18)
			return false;

		if (player.hasEffect(MobEffects.POISON))
			return false;

		if (player.hasEffect(MobEffects.WITHER))
			return false;

		if (player.hasEffect(MobEffects.REGENERATION))
			return false;

		return true;
	}

	private static class OffsetsCache
	{
		protected final Vector<IntPoint> foodBarOffsets = new Vector<>();
		protected final Vector<IntPoint> healthBarOffsets = new Vector<>();
		public int lastGuiTick = 0;

		protected void generate(int guiTicks, Player player)
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

			int healthRows = (int) Math.ceil((float) healthBars / 10.0F);

			int healthRowHeight = Math.max(10 - (healthRows - 2), 3);

			boolean shouldAnimatedFood = false;
			boolean shouldAnimatedHealth = false;

			// when some mods using custom render, we need to least provide an option to cancel animation
			if (ModConfig.SHOW_VANILLA_ANIMATION_OVERLAY.get())
			{
				FoodData stats = player.getFoodData();

				// in vanilla saturation level is zero will show hunger animation
				float saturationLevel = stats.getSaturationLevel();
				int foodLevel = stats.getFoodLevel();
				shouldAnimatedFood = saturationLevel <= 0.0F && guiTicks % (foodLevel * 3 + 1) == 0;

				// in vanilla health is too low (below 5) will show heartbeat animation
				// when regeneration will also show heartbeat animation, but we don't need now
				shouldAnimatedHealth = Math.ceil(player.getHealth()) <= 4;
			}

			// hard code in `InGameHUD`
			random.setSeed((long) (guiTicks * 312871L));

			// adjust the size
			if (foodBarOffsets.size() != preferFoodBars)
				foodBarOffsets.setSize(preferFoodBars);
			if (healthBarOffsets.size() != healthBars)
				healthBarOffsets.setSize(healthBars);

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

		public Vector<IntPoint> healthBarOffsets(int guiTick, Player player)
		{
			if (guiTick != lastGuiTick)
			{
				generate(guiTick, player);
				lastGuiTick = guiTick;
			}
			return this.healthBarOffsets;
		}

		public Vector<IntPoint> foodBarOffsets(int guiTicks, Player player)
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

		protected void query(Player player)
		{
			// try to get the item stack in the player hand
			ItemStack heldItem = player.getMainHandItem();
			FoodHelper.QueriedFoodResult heldFood = FoodHelper.query(heldItem, player);
			boolean canConsume = heldFood != null && FoodHelper.canConsume(player, heldFood.modifiedFoodProperties);
			if (ModConfig.SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND.get() && !canConsume)
			{
				heldItem = player.getOffhandItem();
				heldFood = FoodHelper.query(heldItem, player);
				canConsume = heldFood != null && FoodHelper.canConsume(player, heldFood.modifiedFoodProperties);
			}
			boolean shouldRenderHeldItemValues = !heldItem.isEmpty() && canConsume;
			if (!shouldRenderHeldItemValues)
			{
				this.result = null;
				return;
			}

			this.result = heldFood;
		}

		public FoodHelper.QueriedFoodResult result(int guiTick, Player player)
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
