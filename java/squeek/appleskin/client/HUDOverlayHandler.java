package squeek.appleskin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.GuiOverlayManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.event.HUDOverlayEvent;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.HungerHelper;
import squeek.appleskin.helpers.TextureHelper;
import squeek.appleskin.util.IntPoint;

import java.util.Random;
import java.util.Vector;

@OnlyIn(Dist.CLIENT)
public class HUDOverlayHandler
{
	private static float unclampedFlashAlpha = 0f;
	private static float flashAlpha = 0f;
	private static byte alphaDir = 1;
	protected static int foodIconsOffset;

	public static final Vector<IntPoint> healthBarOffsets = new Vector<>();
	public static final Vector<IntPoint> foodBarOffsets = new Vector<>();

	private static final Random random = new Random();

	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new HUDOverlayHandler());
	}

	static ResourceLocation FOOD_LEVEL_ELEMENT = new ResourceLocation("minecraft", "food_level");
	static ResourceLocation PLAYER_HEALTH_ELEMENT = new ResourceLocation("minecraft", "player_health");

	@SubscribeEvent
	public void onRenderGuiOverlayPre(RenderGuiOverlayEvent.Pre event)
	{
		if (event.getOverlay() == GuiOverlayManager.findOverlay(FOOD_LEVEL_ELEMENT))
		{
			Minecraft mc = Minecraft.getInstance();
			ForgeGui gui = (ForgeGui) mc.gui;
			boolean isMounted = mc.player.getVehicle() instanceof LivingEntity;
			if (!isMounted && !mc.options.hideGui && gui.shouldDrawSurvivalElements())
			{
				renderExhaustion(gui, event.getGuiGraphics(), event.getPartialTick(), event.getWindow().getScreenWidth(), event.getWindow().getScreenHeight());
			}
		}
	}

	@SubscribeEvent
	public void onRenderGuiOverlayPost(RenderGuiOverlayEvent.Post event)
	{
		if (event.getOverlay() == GuiOverlayManager.findOverlay(FOOD_LEVEL_ELEMENT))
		{
			Minecraft mc = Minecraft.getInstance();
			ForgeGui gui = (ForgeGui) mc.gui;
			boolean isMounted = mc.player.getVehicle() instanceof LivingEntity;
			if (!isMounted && !mc.options.hideGui && gui.shouldDrawSurvivalElements())
			{
				renderFoodOrHealthOverlay(gui, event.getGuiGraphics(), event.getPartialTick(), event.getWindow().getScreenWidth(), event.getWindow().getScreenHeight(), RenderOverlayType.FOOD);
			}
		}
		else if (event.getOverlay() == GuiOverlayManager.findOverlay(PLAYER_HEALTH_ELEMENT))
		{
			Minecraft mc = Minecraft.getInstance();
			ForgeGui gui = (ForgeGui) mc.gui;
			if (!mc.options.hideGui && gui.shouldDrawSurvivalElements())
			{
				renderFoodOrHealthOverlay(gui, event.getGuiGraphics(), event.getPartialTick(), event.getWindow().getScreenWidth(), event.getWindow().getScreenHeight(), RenderOverlayType.HEALTH);
			}
		}
	}

	public static void renderExhaustion(ForgeGui gui, GuiGraphics guiGraphics, float partialTicks, int screenWidth, int screenHeight)
	{
		foodIconsOffset = gui.rightHeight;

		if (!ModConfig.SHOW_FOOD_EXHAUSTION_UNDERLAY.get())
			return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		assert player != null;

		int right = mc.getWindow().getGuiScaledWidth() / 2 + 91;
		int top = mc.getWindow().getGuiScaledHeight() - foodIconsOffset;
		float exhaustion = player.getFoodData().getExhaustionLevel();

		// Notify everyone that we should render exhaustion hud overlay
		HUDOverlayEvent.Exhaustion renderEvent = new HUDOverlayEvent.Exhaustion(exhaustion, right, top, guiGraphics);
		MinecraftForge.EVENT_BUS.post(renderEvent);
		if (!renderEvent.isCanceled())
			drawExhaustionOverlay(renderEvent, mc, 1f);
	}

	enum RenderOverlayType
	{
		HEALTH,
		FOOD,
	}

	public static void renderFoodOrHealthOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTicks, int screenWidth, int screenHeight, RenderOverlayType type)
	{
		if (!shouldRenderAnyOverlays())
			return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		assert player != null;
		FoodData stats = player.getFoodData();

		int top = mc.getWindow().getGuiScaledHeight() - foodIconsOffset;
		int left = mc.getWindow().getGuiScaledWidth() / 2 - 91; // left of health bar
		int right = mc.getWindow().getGuiScaledWidth() / 2 + 91; // right of food bar

		if (type == RenderOverlayType.HEALTH)
			generateHealthBarOffsets(top, left, right, mc.gui.getGuiTicks(), player);
		if (type == RenderOverlayType.FOOD)
			generateHungerBarOffsets(top, left, right, mc.gui.getGuiTicks(), player);

		HUDOverlayEvent.Saturation saturationRenderEvent = null;
		if (type == RenderOverlayType.FOOD)
		{
			saturationRenderEvent = new HUDOverlayEvent.Saturation(stats.getSaturationLevel(), right, top, guiGraphics);

			// cancel render overlay event when configuration disabled.
			if (!ModConfig.SHOW_SATURATION_OVERLAY.get())
				saturationRenderEvent.setCanceled(true);

			// notify everyone that we should render saturation hud overlay
			if (!saturationRenderEvent.isCanceled())
				MinecraftForge.EVENT_BUS.post(saturationRenderEvent);

			// the render saturation event maybe cancelled by other mods
			if (!saturationRenderEvent.isCanceled())
				drawSaturationOverlay(saturationRenderEvent, mc, 0, 1f);
		}

		// try to get the item stack in the player hand
		ItemStack heldItem = player.getMainHandItem();
		if (ModConfig.SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND.get() && !FoodHelper.canConsume(heldItem, player))
			heldItem = player.getOffhandItem();

		boolean shouldRenderHeldItemValues = !heldItem.isEmpty() && FoodHelper.canConsume(heldItem, player);
		if (!shouldRenderHeldItemValues)
		{
			resetFlash();
			return;
		}

		FoodValues modifiedFoodValues = FoodHelper.getModifiedFoodValues(heldItem, player);
		FoodValuesEvent foodValuesEvent = new FoodValuesEvent(player, heldItem, FoodHelper.getDefaultFoodValues(heldItem, player), modifiedFoodValues);
		MinecraftForge.EVENT_BUS.post(foodValuesEvent);
		modifiedFoodValues = foodValuesEvent.modifiedFoodValues;

		if (type == RenderOverlayType.HEALTH)
		{
			// Offsets size is set to zero intentionally to disable rendering when health is infinite.
			if (healthBarOffsets.size() == 0)
				return;

			if (!shouldShowEstimatedHealth(heldItem, modifiedFoodValues))
				return;

			float foodHealthIncrement = FoodHelper.getEstimatedHealthIncrement(heldItem, modifiedFoodValues, player);
			float currentHealth = player.getHealth();
			float modifiedHealth = Math.min(currentHealth + foodHealthIncrement, player.getMaxHealth());

			// only create object when the estimated health is successfully
			HUDOverlayEvent.HealthRestored healthRenderEvent = null;
			if (currentHealth < modifiedHealth)
				healthRenderEvent = new HUDOverlayEvent.HealthRestored(modifiedHealth, heldItem, modifiedFoodValues, left, top, guiGraphics);

			// notify everyone that we should render estimated health hud
			if (healthRenderEvent != null)
				MinecraftForge.EVENT_BUS.post(healthRenderEvent);

			if (healthRenderEvent != null && !healthRenderEvent.isCanceled())
				drawHealthOverlay(healthRenderEvent, mc, flashAlpha);
		}
		else if (type == RenderOverlayType.FOOD)
		{
			if (!ModConfig.SHOW_FOOD_VALUES_OVERLAY.get())
				return;

			// notify everyone that we should render hunger hud overlay
			HUDOverlayEvent.HungerRestored renderRenderEvent = new HUDOverlayEvent.HungerRestored(stats.getFoodLevel(), heldItem, modifiedFoodValues, right, top, guiGraphics);
			MinecraftForge.EVENT_BUS.post(renderRenderEvent);
			if (renderRenderEvent.isCanceled())
				return;

			// calculate the final hunger and saturation
			int foodHunger = modifiedFoodValues.hunger;
			float foodSaturationIncrement = modifiedFoodValues.getSaturationIncrement();

			// restored hunger/saturation overlay while holding food
			drawHungerOverlay(renderRenderEvent, mc, foodHunger, flashAlpha, FoodHelper.isRotten(heldItem, player));

			// The render saturation overlay event maybe cancelled by other mods
			assert saturationRenderEvent != null;
			if (!saturationRenderEvent.isCanceled())
			{
				int newFoodValue = stats.getFoodLevel() + foodHunger;
				float newSaturationValue = stats.getSaturationLevel() + foodSaturationIncrement;
				float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - stats.getSaturationLevel() : foodSaturationIncrement;
				// Redraw saturation overlay for gained
				drawSaturationOverlay(saturationRenderEvent, mc, saturationGained, flashAlpha);
			}
		}
	}

	public static void drawSaturationOverlay(float saturationGained, float saturationLevel, Minecraft mc, GuiGraphics guiGraphics, int right, int top, float alpha)
	{
		if (saturationLevel + saturationGained < 0)
			return;

		enableAlpha(alpha);

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

			guiGraphics.blit(TextureHelper.MOD_ICONS, x, y, u, v, iconSize, iconSize);
		}

		// rebind default icons
		RenderSystem.setShaderTexture(0, TextureHelper.MC_ICONS);
		disableAlpha(alpha);
	}

	public static void drawHungerOverlay(int hungerRestored, int foodLevel, Minecraft mc, GuiGraphics guiGraphics, int right, int top, float alpha, boolean useRottenTextures)
	{
		if (hungerRestored <= 0)
			return;

		enableAlpha(alpha);

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
			guiGraphics.blit(TextureHelper.MC_ICONS, x, y, ub, v, iconSize, iconSize);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

			guiGraphics.blit(TextureHelper.MC_ICONS, x, y, u, v, iconSize, iconSize);
		}

		disableAlpha(alpha);
	}

	public static void drawHealthOverlay(float health, float modifiedHealth, Minecraft mc, GuiGraphics guiGraphics, int right, int top, float alpha)
	{
		if (modifiedHealth <= health)
			return;

		enableAlpha(alpha);

		int fixedModifiedHealth = (int) Math.ceil(modifiedHealth);
		boolean isHardcore = mc.player.level() != null && mc.player.level().getLevelData().isHardcore();

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
			guiGraphics.blit(TextureHelper.MC_ICONS, x, y, ub, v, iconSize, iconSize);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);

			guiGraphics.blit(TextureHelper.MC_ICONS, x, y, u, v, iconSize, iconSize);
		}

		disableAlpha(alpha);
	}

	public static void drawExhaustionOverlay(float exhaustion, Minecraft mc, GuiGraphics guiGraphics, int right, int top, float alpha)
	{
		float maxExhaustion = HungerHelper.getMaxExhaustion(mc.player);
		// clamp between 0 and 1
		float ratio = Math.min(1, Math.max(0, exhaustion / maxExhaustion));
		int width = (int) (ratio * 81);
		int height = 9;

		enableAlpha(.75f);
		guiGraphics.blit(TextureHelper.MOD_ICONS, right - width, top, 81 - width, 18, width, height);
		disableAlpha(.75f);

		// rebind default icons
		RenderSystem.setShaderTexture(0, TextureHelper.MC_ICONS);
	}


	public static void enableAlpha(float alpha)
	{
		RenderSystem.enableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, alpha);
		RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	public static void disableAlpha(float alpha)
	{
		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event)
	{
		if (event.phase != TickEvent.Phase.END)
			return;

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

	private static void drawSaturationOverlay(HUDOverlayEvent.Saturation event, Minecraft mc, float saturationGained, float alpha)
	{
		drawSaturationOverlay(saturationGained, event.saturationLevel, mc, event.guiGraphics, event.x, event.y, alpha);
	}

	private static void drawHungerOverlay(HUDOverlayEvent.HungerRestored event, Minecraft mc, int hunger, float alpha, boolean useRottenTextures)
	{
		drawHungerOverlay(hunger, event.currentFoodLevel, mc, event.guiGraphics, event.x, event.y, alpha, useRottenTextures);
	}

	private static void drawHealthOverlay(HUDOverlayEvent.HealthRestored event, Minecraft mc, float alpha)
	{
		drawHealthOverlay(mc.player.getHealth(), event.modifiedHealth, mc, event.guiGraphics, event.x, event.y, alpha);
	}

	private static void drawExhaustionOverlay(HUDOverlayEvent.Exhaustion event, Minecraft mc, float alpha)
	{
		drawExhaustionOverlay(event.exhaustion, mc, event.guiGraphics, event.x, event.y, alpha);
	}

	private static boolean shouldRenderAnyOverlays()
	{
		return ModConfig.SHOW_FOOD_VALUES_OVERLAY.get() || ModConfig.SHOW_SATURATION_OVERLAY.get() || ModConfig.SHOW_FOOD_HEALTH_HUD_OVERLAY.get();
	}

	private static boolean shouldShowEstimatedHealth(ItemStack hoveredStack, FoodValues modifiedFoodValues)
	{
		// then configuration cancel the render event
		if (!ModConfig.SHOW_FOOD_HEALTH_HUD_OVERLAY.get())
			return false;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
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

	private static void generateHealthBarOffsets(int top, int left, int right, int ticks, Player player)
	{
		// hard code in `InGameHUD`
		random.setSeed((long) (ticks * 312871L));

		final int preferHealthBars = 10;
		final float maxHealth = player.getMaxHealth();
		final float absorptionHealth = (float) Math.ceil(player.getAbsorptionAmount());

		int healthBars = (int) Math.ceil((maxHealth + absorptionHealth) / 2.0F);
		// When maxHealth + absorptionHealth is greater than Integer.INT_MAX,
		// Minecraft will disable heart rendering due to a quirk of MathHelper.ceil.
		// We have a much lower threshold since there's no reason to get the offsets
		// for thousands of hearts.
		// Note: Infinite and > INT_MAX absorption has been seen in the wild.
		// This will effectively disable rendering whenever health is unexpectedly large.
		if (healthBars < 0 || healthBars > 1000) {
			healthBarOffsets.setSize(0);
			return;
		}

		int healthRows = (int) Math.ceil((float) healthBars / 10.0F);

		int healthRowHeight = Math.max(10 - (healthRows - 2), 3);

		boolean shouldAnimatedHealth = false;

		// when some mods using custom render, we need to least provide an option to cancel animation
		if (ModConfig.SHOW_VANILLA_ANIMATION_OVERLAY.get())
		{
			// in vanilla health is too low (below 5) will show heartbeat animation
			// when regeneration will also show heartbeat animation, but we don't need now
			shouldAnimatedHealth = Math.ceil(player.getHealth()) <= 4;
		}

		// adjust the size
		if (healthBarOffsets.size() != healthBars)
			healthBarOffsets.setSize(healthBars);

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
	}

	private static void generateHungerBarOffsets(int top, int left, int right, int ticks, Player player)
	{
		final int preferFoodBars = 10;

		boolean shouldAnimatedFood = false;

		// when some mods using custom render, we need to least provide an option to cancel animation
		if (ModConfig.SHOW_VANILLA_ANIMATION_OVERLAY.get())
		{
			FoodData stats = player.getFoodData();

			// in vanilla saturation level is zero will show hunger animation
			float saturationLevel = stats.getSaturationLevel();
			int foodLevel = stats.getFoodLevel();
			shouldAnimatedFood = saturationLevel <= 0.0F && ticks % (foodLevel * 3 + 1) == 0;
		}

		if (foodBarOffsets.size() != preferFoodBars)
			foodBarOffsets.setSize(preferFoodBars);

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
