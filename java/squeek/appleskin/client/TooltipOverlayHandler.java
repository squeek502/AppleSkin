package squeek.appleskin.client;

import com.mojang.datafixers.util.Either;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix3x2fStack;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.TooltipOverlayEvent;
import squeek.appleskin.helpers.ColorHelper;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.KeyHelper;
import squeek.appleskin.helpers.TextureHelper;

public class TooltipOverlayHandler
{
	public static void init()
	{
		NeoForge.EVENT_BUS.register(new TooltipOverlayHandler());
	}

	public static void register(RegisterClientTooltipComponentFactoriesEvent event)
	{
		event.register(FoodTooltip.class, FoodTooltipRenderer::new);
	}

	enum FoodOutline
	{
		NEGATIVE,
		EXTRA,
		NORMAL,
		PARTIAL,
		MISSING;

		public int argb()
		{
			return switch (this)
			{
				case NEGATIVE -> ColorHelper.argbFromRGBA(1.0f, 1.0f, 1.0f, 1.0f);
				case EXTRA -> ColorHelper.argbFromRGBA(0.06f, 0.32f, 0.02f, 1.0f);
				case NORMAL -> ColorHelper.argbFromRGBA(0.0f, 0.0f, 0.0f, 1.0f);
				case PARTIAL -> ColorHelper.argbFromRGBA(0.53f, 0.21f, 0.08f, 1.0f);
				case MISSING -> ColorHelper.argbFromRGBA(0.62f, 0.0f, 0.0f, 0.5f);
			};
		}

		public static FoodOutline get(int modifiedFoodHunger, int defaultFoodHunger, int i)
		{
			if (modifiedFoodHunger < 0)
				return NEGATIVE;
			else if (modifiedFoodHunger > defaultFoodHunger && defaultFoodHunger <= i)
				return EXTRA;
			else if (modifiedFoodHunger > i + 1 || defaultFoodHunger == modifiedFoodHunger)
				return NORMAL;
			else if (modifiedFoodHunger == i + 1)
				return PARTIAL;
			else
				return MISSING;
		}
	}

	static class FoodTooltipRenderer implements ClientTooltipComponent
	{
		private FoodTooltip foodTooltip;

		FoodTooltipRenderer(FoodTooltip foodTooltip)
		{
			this.foodTooltip = foodTooltip;
		}

		@Override
		public int getHeight(Font font)
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
		public void renderImage(Font font, int x, int y, int width, int height, GuiGraphics guiGraphics)
		{
			ItemStack itemStack = foodTooltip.itemStack;
			Minecraft mc = Minecraft.getInstance();
			if (!shouldShowTooltip(itemStack, mc.player))
				return;

			Screen gui = mc.screen;
			if (gui == null)
				return;

			FoodProperties defaultFood = foodTooltip.defaultFood;
			FoodProperties modifiedFood = foodTooltip.modifiedFood;

			// Notify everyone that we should render tooltip overlay
			TooltipOverlayEvent.Render renderEvent = new TooltipOverlayEvent.Render(itemStack, x, y, guiGraphics, defaultFood, modifiedFood);
			NeoForge.EVENT_BUS.post(renderEvent);
			if (renderEvent.isCanceled())
				return;

			x = renderEvent.x;
			y = renderEvent.y;
			guiGraphics = renderEvent.guiGraphics;

			int offsetX = x;
			int offsetY = y;

			int defaultHunger = defaultFood.nutrition();
			int modifiedHunger = modifiedFood.nutrition();

			// Render from right to left so that the icons 'face' the right way
			offsetX += (foodTooltip.hungerBars - 1) * 9;

			boolean isRotten = FoodHelper.isRotten(foodTooltip.consumable);

			for (int i = 0; i < foodTooltip.hungerBars * 2; i += 2)
			{
				guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.FOOD_EMPTY_TEXTURE, offsetX, offsetY, 9, 9);

				FoodOutline outline = FoodOutline.get(modifiedHunger, defaultHunger, i);
				if (outline != FoodOutline.NORMAL)
				{
					guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.HUNGER_OUTLINE_SPRITE, offsetX, offsetY, 9, 9, outline.argb());
				}

				boolean isDefaultHalf = defaultHunger - 1 == i;
				Identifier defaultFoodIcon = TextureHelper.getFoodTexture(isRotten, isDefaultHalf ? TextureHelper.FoodType.HALF : TextureHelper.FoodType.FULL);
				guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, defaultFoodIcon, offsetX, offsetY, 9, 9, ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.25F));

				if (modifiedHunger > i)
				{
					boolean isModifiedHalf = modifiedHunger - 1 == i;
					Identifier modifiedFoodIcon = TextureHelper.getFoodTexture(isRotten, isModifiedHalf ? TextureHelper.FoodType.HALF : TextureHelper.FoodType.FULL);
					guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, modifiedFoodIcon, offsetX, offsetY, 9, 9);
				}

				offsetX -= 9;
			}

			if (foodTooltip.hungerBarsText != null)
			{
				offsetX += 18;
				Matrix3x2fStack poseStack = guiGraphics.pose();
				poseStack.pushMatrix();
				poseStack.translate(offsetX, offsetY);
				poseStack.scale(0.75f, 0.75f);
				guiGraphics.drawString(font, foodTooltip.hungerBarsText, 2, 2, 0xFFAAAAAA);
				poseStack.popMatrix();
			}

			offsetX = x;
			offsetY += 10;

			float modifiedSaturationIncrement = modifiedFood.saturation();
			float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

			// Render from right to left so that the icons 'face' the right way
			offsetX += (foodTooltip.saturationBars - 1) * 7;

			for (int i = 0; i < foodTooltip.saturationBars * 2; i += 2)
			{
				float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

				boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
				int color = shouldBeFaded ? ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.5F) : ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 1.0F);
				guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, offsetX, offsetY, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7, 256, 256, color);

				offsetX -= 7;
			}
			if (foodTooltip.saturationBarsText != null)
			{
				offsetX += 14;
				Matrix3x2fStack poseStack = guiGraphics.pose();
				poseStack.pushMatrix();
				poseStack.translate(offsetX, offsetY);
				poseStack.scale(0.75f, 0.75f);
				guiGraphics.drawString(font, foodTooltip.saturationBarsText, 2, 1, 0xFFAAAAAA);
				poseStack.popMatrix();
			}
		}
	}

	static class FoodTooltip implements TooltipComponent
	{
		private FoodProperties defaultFood;
		private FoodProperties modifiedFood;
		private Consumable consumable;

		private int biggestHunger;
		private float biggestSaturationIncrement;

		private int hungerBars;
		private String hungerBarsText;

		private int saturationBars;
		private String saturationBarsText;

		private ItemStack itemStack;

		FoodTooltip(ItemStack itemStack, FoodProperties defaultFood, FoodProperties modifiedFood, Consumable consumable, Player player)
		{
			this.itemStack = itemStack;
			this.defaultFood = defaultFood;
			this.modifiedFood = modifiedFood;
			this.consumable = consumable;

			biggestHunger = Math.max(defaultFood.nutrition(), modifiedFood.nutrition());
			biggestSaturationIncrement = Math.max(defaultFood.saturation(), modifiedFood.saturation());

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

		FoodHelper.QueriedFoodResult queriedFoodResult = FoodHelper.query(hoveredStack, mc.player);
		if (queriedFoodResult == null)
			return;

		FoodProperties defaultFood = queriedFoodResult.defaultFoodProperties;
		FoodProperties modifiedFood = queriedFoodResult.modifiedFoodProperties;

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Pre prerenderEvent = new TooltipOverlayEvent.Pre(hoveredStack, defaultFood, modifiedFood);
		NeoForge.EVENT_BUS.post(prerenderEvent);
		if (prerenderEvent.isCanceled())
			return;

		FoodTooltip foodTooltip = new FoodTooltip(prerenderEvent.itemStack, defaultFood, modifiedFood, queriedFoodResult.consumable, mc.player);
		if (foodTooltip.shouldRenderHungerBars())
			event.getTooltipElements().add(Either.right(foodTooltip));
	}

	private static boolean shouldShowTooltip(ItemStack hoveredStack, Player player)
	{
		if (hoveredStack.isEmpty())
			return false;

		// Note: The intention here is to match the conditional in ItemStack.getTooltipLines, but
		//       the NeoForge event does not provide TooltipFlags so this does not match exactly.
		// TODO: (!tooltipFlags.isCreative() && ...) if NeoForge starts providing TooltipFlags
		if (hoveredStack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).hideTooltip())
			return false;

		boolean shouldShowTooltip = (ModConfig.SHOW_FOOD_VALUES_IN_TOOLTIP.get() && KeyHelper.isShiftKeyDown()) || ModConfig.ALWAYS_SHOW_FOOD_VALUES_TOOLTIP.get();
		if (!shouldShowTooltip)
			return false;

		if (!FoodHelper.isFood(hoveredStack, player))
			return false;

		return true;
	}
}
