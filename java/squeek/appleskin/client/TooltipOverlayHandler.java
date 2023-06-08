package squeek.appleskin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.event.TooltipOverlayEvent;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.KeyHelper;
import squeek.appleskin.helpers.TextureHelper;

@OnlyIn(Dist.CLIENT)
public class TooltipOverlayHandler
{
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_BOTTOM = 3;
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_TOP = -3;
	public static final int TOOLTIP_REAL_WIDTH_OFFSET_RIGHT = 3;

	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new TooltipOverlayHandler());
	}

	public static void register(RegisterClientTooltipComponentFactoriesEvent event)
	{
		event.register(FoodTooltip.class, FoodTooltipRenderer::new);
	}

	private static final TextureOffsets normalBarTextureOffsets = new TextureOffsets();

	static
	{
		normalBarTextureOffsets.containerNegativeHunger = 43;
		normalBarTextureOffsets.containerExtraHunger = 133;
		normalBarTextureOffsets.containerNormalHunger = 16;
		normalBarTextureOffsets.containerPartialHunger = 124;
		normalBarTextureOffsets.containerMissingHunger = 34;
		normalBarTextureOffsets.shankMissingFull = 70;
		normalBarTextureOffsets.shankMissingPartial = normalBarTextureOffsets.shankMissingFull + 9;
		normalBarTextureOffsets.shankFull = 52;
		normalBarTextureOffsets.shankPartial = normalBarTextureOffsets.shankFull + 9;
	}

	private static final TextureOffsets rottenBarTextureOffsets = new TextureOffsets();

	static
	{
		rottenBarTextureOffsets.containerNegativeHunger = normalBarTextureOffsets.containerNegativeHunger;
		rottenBarTextureOffsets.containerExtraHunger = normalBarTextureOffsets.containerExtraHunger;
		rottenBarTextureOffsets.containerNormalHunger = normalBarTextureOffsets.containerNormalHunger;
		rottenBarTextureOffsets.containerPartialHunger = normalBarTextureOffsets.containerPartialHunger;
		rottenBarTextureOffsets.containerMissingHunger = normalBarTextureOffsets.containerMissingHunger;
		rottenBarTextureOffsets.shankMissingFull = 106;
		rottenBarTextureOffsets.shankMissingPartial = rottenBarTextureOffsets.shankMissingFull + 9;
		rottenBarTextureOffsets.shankFull = 88;
		rottenBarTextureOffsets.shankPartial = rottenBarTextureOffsets.shankFull + 9;
	}

	static class TextureOffsets
	{
		int containerNegativeHunger;
		int containerExtraHunger;
		int containerNormalHunger;
		int containerPartialHunger;
		int containerMissingHunger;
		int shankMissingFull;
		int shankMissingPartial;
		int shankFull;
		int shankPartial;
	}

	static class FoodTooltipRenderer implements ClientTooltipComponent
	{
		private FoodTooltip foodTooltip;

		FoodTooltipRenderer(FoodTooltip foodTooltip)
		{
			this.foodTooltip = foodTooltip;
		}

		@Override
		public int getHeight()
		{
			// hunger + spacing + saturation + arbitrary spacing,
			// for some reason 3 extra looks best
			return 9 + 1 + 7 + 3;
		}

		@Override
		public int getWidth(Font font)
		{
			int hungerBarsWidth = foodTooltip.hungerBars * 9;
			if (foodTooltip.hungerBarsText != null)
				hungerBarsWidth += font.width(foodTooltip.hungerBarsText);

			int saturationBarsWidth = foodTooltip.saturationBars * 7;
			if (foodTooltip.saturationBarsText != null)
				saturationBarsWidth += font.width(foodTooltip.saturationBarsText);

			return Math.max(hungerBarsWidth, saturationBarsWidth) + 2; // right padding
		}

		@Override
		public void renderImage(Font font, int x, int y, GuiGraphics guiGraphics)
		{
			ItemStack itemStack = foodTooltip.itemStack;
			Minecraft mc = Minecraft.getInstance();
			if (!shouldShowTooltip(itemStack, mc.player))
				return;

			Screen gui = mc.screen;
			if (gui == null)
				return;

			FoodValues defaultFood = foodTooltip.defaultFood;
			FoodValues modifiedFood = foodTooltip.modifiedFood;

			// Notify everyone that we should render tooltip overlay
			TooltipOverlayEvent.Render renderEvent = new TooltipOverlayEvent.Render(itemStack, x, y, guiGraphics, defaultFood, modifiedFood);
			MinecraftForge.EVENT_BUS.post(renderEvent);
			if (renderEvent.isCanceled())
				return;

			x = renderEvent.x;
			y = renderEvent.y;
			guiGraphics = renderEvent.guiGraphics;

			RenderSystem.enableDepthTest();
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();

			int offsetX = x;
			int offsetY = y;

			int defaultHunger = defaultFood.hunger;
			int modifiedHunger = modifiedFood.hunger;

			// Render from right to left so that the icons 'face' the right way
			offsetX += (foodTooltip.hungerBars - 1) * 9;

			TextureOffsets offsets = FoodHelper.isRotten(itemStack, mc.player) ? rottenBarTextureOffsets : normalBarTextureOffsets;
			for (int i = 0; i < foodTooltip.hungerBars * 2; i += 2)
			{

				if (modifiedHunger < 0)
					guiGraphics.blit(TextureHelper.MC_ICONS, offsetX, offsetY, 0, offsets.containerNegativeHunger, 27, 9, 9, 256, 256);
				else if (modifiedHunger > defaultHunger && defaultHunger <= i)
					guiGraphics.blit(TextureHelper.MC_ICONS, offsetX, offsetY, 0, offsets.containerExtraHunger, 27, 9, 9, 256, 256);
				else if (modifiedHunger > i + 1 || defaultHunger == modifiedHunger)
					guiGraphics.blit(TextureHelper.MC_ICONS, offsetX, offsetY, 0, offsets.containerNormalHunger, 27, 9, 9, 256, 256);
				else if (modifiedHunger == i + 1)
					guiGraphics.blit(TextureHelper.MC_ICONS, offsetX, offsetY, 0, offsets.containerPartialHunger, 27, 9, 9, 256, 256);
				else
				{
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, .5F);
					guiGraphics.blit(TextureHelper.MC_ICONS, offsetX, offsetY, 0, offsets.containerMissingHunger, 27, 9, 9, 256, 256);
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
				}

				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, .25F);
				guiGraphics.blit(TextureHelper.MC_ICONS, offsetX, offsetY, 0, defaultHunger - 1 == i ? offsets.shankMissingPartial : offsets.shankMissingFull, 27, 9, 9, 256, 256);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

				if (modifiedHunger > i)
					guiGraphics.blit(TextureHelper.MC_ICONS, offsetX, offsetY, 0, modifiedHunger - 1 == i ? offsets.shankPartial : offsets.shankFull, 27, 9, 9, 256, 256);

				offsetX -= 9;
			}
			if (foodTooltip.hungerBarsText != null)
			{
				offsetX += 18;
				PoseStack poseStack = guiGraphics.pose();
				poseStack.pushPose();
				poseStack.translate(offsetX, offsetY, 0);
				poseStack.scale(0.75f, 0.75f, 0.75f);
				guiGraphics.drawCenteredString(font, foodTooltip.hungerBarsText, 2, 2, 0xFFAAAAAA);
				poseStack.popPose();
			}

			offsetX = x;
			offsetY += 10;

			float modifiedSaturationIncrement = modifiedFood.getSaturationIncrement();
			float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

			// Render from right to left so that the icons 'face' the right way
			offsetX += (foodTooltip.saturationBars - 1) * 7;

			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			for (int i = 0; i < foodTooltip.saturationBars * 2; i += 2)
			{
				float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

				boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
				if (shouldBeFaded)
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, .5F);

				guiGraphics.blit(TextureHelper.MOD_ICONS, offsetX, offsetY, 0, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7, 256, 256);

				if (shouldBeFaded)
					RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

				offsetX -= 7;
			}
			if (foodTooltip.saturationBarsText != null)
			{
				offsetX += 14;
				PoseStack poseStack = guiGraphics.pose();
				poseStack.pushPose();
				poseStack.translate(offsetX, offsetY, 0);
				poseStack.scale(0.75f, 0.75f, 0.75f);
				guiGraphics.drawCenteredString(font, foodTooltip.saturationBarsText, 2, 1, 0xFFAAAAAA);
				poseStack.popPose();
			}

			RenderSystem.disableBlend();
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.setShaderTexture(0, TextureHelper.MC_ICONS);

			// reset to drawHoveringText state
			RenderSystem.disableDepthTest();
		}
	}

	static class FoodTooltip implements TooltipComponent
	{
		private FoodValues defaultFood;
		private FoodValues modifiedFood;

		private int biggestHunger;
		private float biggestSaturationIncrement;

		private int hungerBars;
		private String hungerBarsText;

		private int saturationBars;
		private String saturationBarsText;

		private ItemStack itemStack;

		FoodTooltip(ItemStack itemStack, FoodValues defaultFood, FoodValues modifiedFood, Player player)
		{
			this.itemStack = itemStack;
			this.defaultFood = defaultFood;
			this.modifiedFood = modifiedFood;

			biggestHunger = Math.max(defaultFood.hunger, modifiedFood.hunger);
			biggestSaturationIncrement = Math.max(defaultFood.getSaturationIncrement(), modifiedFood.getSaturationIncrement());

			hungerBars = (int) Math.ceil(Math.abs(biggestHunger) / 2f);
			if (hungerBars > 10)
			{
				hungerBarsText = "x" + ((biggestHunger < 0 ? -1 : 1) * hungerBars);
				hungerBars = 1;
			}

			saturationBars = (int) Math.ceil(Math.abs(biggestSaturationIncrement) / 2f);
			if (saturationBars > 10 || saturationBars == 0)
			{
				saturationBarsText = "x" + ((biggestSaturationIncrement < 0 ? -1 : 1) * saturationBars);
				saturationBars = 1;
			}
		}

		boolean shouldRenderHungerBars()
		{
			return hungerBars > 0;
		}
	}

	@SubscribeEvent
	public void gatherTooltips(RenderTooltipEvent.GatherComponents event)
	{
		if (event.isCanceled())
			return;

		ItemStack hoveredStack = event.getItemStack();
		Minecraft mc = Minecraft.getInstance();
		if (!shouldShowTooltip(hoveredStack, mc.player))
			return;

		FoodValues defaultFood = FoodHelper.getDefaultFoodValues(hoveredStack, mc.player);
		FoodValues modifiedFood = FoodHelper.getModifiedFoodValues(hoveredStack, mc.player);

		FoodValuesEvent foodValuesEvent = new FoodValuesEvent(mc.player, hoveredStack, defaultFood, modifiedFood);
		MinecraftForge.EVENT_BUS.post(foodValuesEvent);
		defaultFood = foodValuesEvent.defaultFoodValues;
		modifiedFood = foodValuesEvent.modifiedFoodValues;

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Pre prerenderEvent = new TooltipOverlayEvent.Pre(hoveredStack, defaultFood, modifiedFood);
		MinecraftForge.EVENT_BUS.post(prerenderEvent);
		if (prerenderEvent.isCanceled())
			return;

		FoodTooltip foodTooltip = new FoodTooltip(prerenderEvent.itemStack, defaultFood, modifiedFood, mc.player);
		if (foodTooltip.shouldRenderHungerBars())
			event.getTooltipElements().add(Either.right(foodTooltip));
	}

	private static boolean shouldShowTooltip(ItemStack hoveredStack, Player player)
	{
		if (hoveredStack.isEmpty())
			return false;

		boolean shouldShowTooltip = (ModConfig.SHOW_FOOD_VALUES_IN_TOOLTIP.get() && KeyHelper.isShiftKeyDown()) || ModConfig.ALWAYS_SHOW_FOOD_VALUES_TOOLTIP.get();
		if (!shouldShowTooltip)
			return false;

		if (!FoodHelper.isFood(hoveredStack, player))
			return false;

		return true;
	}
}
