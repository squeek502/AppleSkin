package squeek.appleskin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.FoodStats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import squeek.appleskin.ModConfig;
import squeek.appleskin.ModInfo;
import squeek.appleskin.api.food.IFood;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.HungerHelper;
import squeek.appleskin.api.events.HUDOverlayEvent;

@OnlyIn(Dist.CLIENT)
public class HUDOverlayHandler
{
	private float flashAlpha = 0f;
	private byte alphaDir = 1;
	protected int foodIconsOffset;

	private static final ResourceLocation modIcons = new ResourceLocation(ModInfo.MODID_LOWER, "textures/icons.png");

	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new HUDOverlayHandler());
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public void onPreRender(RenderGameOverlayEvent.Pre event)
	{
		if (event.getType() != RenderGameOverlayEvent.ElementType.FOOD)
			return;

		foodIconsOffset = ForgeIngameGui.right_height;

		if (event.isCanceled())
			return;

		if (!ModConfig.SHOW_FOOD_EXHAUSTION_UNDERLAY.get())
			return;

		Minecraft mc = Minecraft.getInstance();
		PlayerEntity player = mc.player;

		int left = mc.getMainWindow().getScaledWidth() / 2 + 91;
		int top = mc.getMainWindow().getScaledHeight() - foodIconsOffset;
		float exhaustion = HungerHelper.getExhaustion(player);

		// Notify everyone that we should render exhaustion hud overlay
		HUDOverlayEvent.Exhaustion renderEvent = new HUDOverlayEvent.Exhaustion(exhaustion, left, top, event.getMatrixStack());
		MinecraftForge.EVENT_BUS.post(renderEvent);
		if (!renderEvent.isCanceled()) {
			drawExhaustionOverlay(renderEvent, 1f, mc);
		}
	}

	@SubscribeEvent(priority = EventPriority.LOW)
	public void onRender(RenderGameOverlayEvent.Post event)
	{
		if (event.getType() != RenderGameOverlayEvent.ElementType.FOOD)
			return;

		if (event.isCanceled())
			return;

		if (!ModConfig.SHOW_FOOD_VALUES_OVERLAY.get() && !ModConfig.SHOW_SATURATION_OVERLAY.get())
			return;

		Minecraft mc = Minecraft.getInstance();
		PlayerEntity player = mc.player;
		FoodStats stats = player.getFoodStats();

		int left = mc.getMainWindow().getScaledWidth() / 2 + 91;
		int top = mc.getMainWindow().getScaledHeight() - foodIconsOffset;
		float saturationLevel = stats.getSaturationLevel();

		HUDOverlayEvent.Pre prerenderEvent = new HUDOverlayEvent.Pre(left, top, event.getMatrixStack());
		MinecraftForge.EVENT_BUS.post(prerenderEvent);
		if (prerenderEvent.isCanceled()) {
			return;
		}

		HUDOverlayEvent.Saturation saturationRenderEvent = new HUDOverlayEvent.Saturation(saturationLevel, left, top, event.getMatrixStack());

		// Cancel render overlay event when configuration disabled.
		if (!ModConfig.SHOW_SATURATION_OVERLAY.get()) {
			saturationRenderEvent.setCanceled(true);
		}

		// Notify everyone that we should render saturation hud overlay
		if (!saturationRenderEvent.isCanceled()) {
			MinecraftForge.EVENT_BUS.post(saturationRenderEvent);
		}

		// The render saturation event maybe cancelled by other mods
		if (!saturationRenderEvent.isCanceled()) {
			drawSaturationOverlay(saturationRenderEvent, 0, 1f, mc);
		}

		ItemStack heldItem = player.getHeldItemMainhand();
		if (ModConfig.SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND.get() && !FoodHelper.isFood(heldItem)) {
			heldItem = player.getHeldItemOffhand();
		}
		if (!ModConfig.SHOW_FOOD_VALUES_OVERLAY.get() || heldItem.isEmpty() || !FoodHelper.isFood(heldItem)) {
			flashAlpha = 0;
			alphaDir = 1;
			return;
		}

		int foodLevel = stats.getFoodLevel();
		IFood modifiedFood = FoodHelper.getModifiedFoodValues(heldItem, player);

		// Notify everyone that we should render hunger hud overlay
		HUDOverlayEvent.Hunger renderRenderEvent = new HUDOverlayEvent.Hunger(foodLevel, heldItem, modifiedFood, left, top, event.getMatrixStack());
		MinecraftForge.EVENT_BUS.post(renderRenderEvent);
		if (renderRenderEvent.isCanceled()) {
			flashAlpha = 0;
			alphaDir = 1;
			return;
		}
		modifiedFood = renderRenderEvent.modifiedFood;

		// restored hunger/saturation overlay while holding food
		drawHungerOverlay(renderRenderEvent, flashAlpha, FoodHelper.isRotten(heldItem), mc);

		// The render saturation overlay event maybe cancelled by other mods
		if (!saturationRenderEvent.isCanceled()) {
			int newFoodValue = stats.getFoodLevel() + modifiedFood.getHunger(heldItem, player);
			float newSaturationValue = saturationLevel + modifiedFood.getSaturationIncrement(heldItem, player);
			float saturationGained = newSaturationValue > newFoodValue ? newFoodValue - saturationLevel : modifiedFood.getSaturationIncrement(heldItem, player);
			// Redraw saturation overlay for gained
			drawSaturationOverlay(saturationRenderEvent, saturationGained, flashAlpha, mc);
		}
	}

	public static void drawSaturationOverlay(float saturationGained, float saturationLevel, Minecraft mc, MatrixStack matrixStack, int left, int top, float alpha)
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
				mc.ingameGUI.blit(matrixStack, x, y, 27, 0, 9, 9);
			else if (effectiveSaturationOfBar > .5)
				mc.ingameGUI.blit(matrixStack, x, y, 18, 0, 9, 9);
			else if (effectiveSaturationOfBar > .25)
				mc.ingameGUI.blit(matrixStack, x, y, 9, 0, 9, 9);
			else if (effectiveSaturationOfBar > 0)
				mc.ingameGUI.blit(matrixStack, x, y, 0, 0, 9, 9);
		}
		disableAlpha(alpha);

		// rebind default icons
		mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
	}

	public static void drawHungerOverlay(int hungerRestored, int foodLevel, Minecraft mc, MatrixStack matrixStack, int left, int top, float alpha, boolean useRottenTextures)
	{
		if (hungerRestored == 0)
			return;

		int startBar = foodLevel / 2;
		int endBar = (int) Math.ceil(Math.min(20, foodLevel + hungerRestored) / 2f);
		int barsNeeded = endBar - startBar;
		mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);

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
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha * 0.1f);
			mc.ingameGUI.blit(matrixStack, x, y, 16 + background * 9, 27, 9, 9);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);

			if (idx < foodLevel + hungerRestored)
				mc.ingameGUI.blit(matrixStack, x, y, icon + 36, 27, 9, 9);
			else if (idx == foodLevel + hungerRestored)
				mc.ingameGUI.blit(matrixStack, x, y, icon + 45, 27, 9, 9);
		}
		disableAlpha(alpha);
	}

	public static void drawExhaustionOverlay(float exhaustion, Minecraft mc, MatrixStack matrixStack, int left, int top, float alpha)
	{
		mc.getTextureManager().bindTexture(modIcons);

		float maxExhaustion = HungerHelper.getMaxExhaustion(mc.player);
		// clamp between 0 and 1
		float ratio = Math.min(1, Math.max(0, exhaustion / maxExhaustion));
		int width = (int) (ratio * 81);
		int height = 9;

		enableAlpha(.75f);
		mc.ingameGUI.blit(matrixStack, left - width, top, 81 - width, 18, width, height);
		disableAlpha(.75f);

		// rebind default icons
		mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
	}

	public static void enableAlpha(float alpha)
	{
		RenderSystem.enableBlend();

		if (alpha == 1f)
			return;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, alpha);
		RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
	}

	public static void disableAlpha(float alpha)
	{
		RenderSystem.disableBlend();

		if (alpha == 1f)
			return;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event)
	{
		if (event.phase != TickEvent.Phase.END)
			return;

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

	private static void drawExhaustionOverlay(HUDOverlayEvent.Exhaustion event, float alpha, Minecraft mc)
	{
		drawExhaustionOverlay(event.exhaustion, mc, event.matrixStack, event.x, event.y, alpha);
	}
	private static void drawSaturationOverlay(HUDOverlayEvent.Saturation event, float saturationGained, float alpha, Minecraft mc)
	{
		drawSaturationOverlay(saturationGained, event.saturationLevel, mc, event.matrixStack, event.x, event.y, alpha);
	}
	private static void drawHungerOverlay(HUDOverlayEvent.Hunger event, float alpha, boolean useRottenTextures, Minecraft mc)
	{
		int hunger = event.modifiedFood.getHunger(event.itemStack, mc.player);
		drawHungerOverlay(hunger, event.foodLevel, mc, event.matrixStack, event.x, event.y, alpha, useRottenTextures);
	}
}
