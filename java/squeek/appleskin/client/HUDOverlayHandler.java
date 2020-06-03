package squeek.appleskin.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Effects;
import net.minecraft.util.FoodStats;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.gui.ForgeIngameGui;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import squeek.appleskin.ModConfig;
import squeek.appleskin.ModInfo;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.HungerHelper;

@OnlyIn(Dist.CLIENT)
public class HUDOverlayHandler
{
	private float flashAlpha = 0f;
	private byte alphaDir = 1;
	protected int foodIconsOffset;

	private static final ResourceLocation modIcons = new ResourceLocation(ModInfo.MODID_LOWER, "textures/icons.png");

	final int[] normalSaturationTextures = new int[] {27, 18, 9, 0};
	final int[] rottenSaturationTextures = new int[] {63, 54, 45, 36};

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

		int left = mc.func_228018_at_().getScaledWidth() / 2 + 91;
		int top = mc.func_228018_at_().getScaledHeight() - foodIconsOffset;

		drawExhaustionOverlay(HungerHelper.getExhaustion(player), mc, left, top, 1f);
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
		ItemStack heldItem = player.getHeldItemMainhand();
		if(!FoodHelper.isFood(heldItem))
			heldItem = player.getHeldItemOffhand();
		FoodStats stats = player.getFoodStats();

		int left = mc.func_228018_at_().getScaledWidth() / 2 + 91;
		int top = mc.func_228018_at_().getScaledHeight() - foodIconsOffset;

		// saturation overlay
		if (ModConfig.SHOW_SATURATION_OVERLAY.get())
			drawSaturationOverlay(0, stats.getSaturationLevel(), mc, left, top, 1f, normalSaturationTextures);

		if (!ModConfig.SHOW_FOOD_VALUES_OVERLAY.get() || heldItem.isEmpty() || !FoodHelper.isFood(heldItem))
		{
			flashAlpha = 0;
			alphaDir = 1;
			return;
		}

		// restored hunger/saturation overlay while holding food
		FoodHelper.BasicFoodValues foodValues = FoodHelper.getModifiedFoodValues(heldItem, player);
		drawHungerOverlay(foodValues.hunger, stats.getFoodLevel(), mc, left, top, flashAlpha);

		if (ModConfig.SHOW_SATURATION_OVERLAY.get())
		{
			int newFoodValue = stats.getFoodLevel() + foodValues.hunger;
			float newSaturationValue = stats.getSaturationLevel() + foodValues.getSaturationIncrement();
			drawSaturationOverlay(
					newSaturationValue > newFoodValue ? newFoodValue - stats.getSaturationLevel() : foodValues.getSaturationIncrement(),
					stats.getSaturationLevel(),
					mc,
					left,
					top,
					flashAlpha,
					FoodHelper.isRotten(heldItem) ? rottenSaturationTextures : normalSaturationTextures);
		}
	}

	public static void drawSaturationOverlay(float saturationGained, float saturationLevel, Minecraft mc, int left, int top, float alpha, int[] textureXPositions)
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
				mc.ingameGUI.blit(x, y, textureXPositions[0], 0, 9, 9);
			else if (effectiveSaturationOfBar > .5)
				mc.ingameGUI.blit(x, y, textureXPositions[1], 0, 9, 9);
			else if (effectiveSaturationOfBar > .25)
				mc.ingameGUI.blit(x, y, textureXPositions[2], 0, 9, 9);
			else if (effectiveSaturationOfBar > 0)
				mc.ingameGUI.blit(x, y, textureXPositions[3], 0, 9, 9);
		}
		disableAlpha(alpha);

		// rebind default icons
		mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
	}

	public static void drawHungerOverlay(int hungerRestored, int foodLevel, Minecraft mc, int left, int top, float alpha)
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
			int background = 13;

			if (mc.player.isPotionActive(Effects.HUNGER))
			{
				icon += 36;
				background = 13;
			}

			mc.ingameGUI.blit(x, y, 16 + background * 9, 27, 9, 9);

			if (idx < foodLevel + hungerRestored)
				mc.ingameGUI.blit(x, y, icon + 36, 27, 9, 9);
			else if (idx == foodLevel + hungerRestored)
				mc.ingameGUI.blit(x, y, icon + 45, 27, 9, 9);
		}
		disableAlpha(alpha);
	}

	public static void drawExhaustionOverlay(float exhaustion, Minecraft mc, int left, int top, float alpha)
	{
		mc.getTextureManager().bindTexture(modIcons);

		float maxExhaustion = HungerHelper.getMaxExhaustion(mc.player);
		float ratio = exhaustion / maxExhaustion;
		int width = (int) (ratio * 81);
		int height = 9;

		enableAlpha(.75f);
		mc.ingameGUI.blit(left - width, top, 81 - width, 18, width, height);
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
}
