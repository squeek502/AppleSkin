package squeek.appleskin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.TextVisitFactory;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.item.TooltipData;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.event.TooltipOverlayEvent;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.KeyHelper;
import squeek.appleskin.helpers.TextureHelper;
import squeek.appleskin.helpers.TextureHelper.FoodType;

import java.util.ArrayList;
import java.util.List;

public class TooltipOverlayHandler
{
	public static TooltipOverlayHandler INSTANCE;

	static abstract class EmptyText implements Text
	{
		@Override
		public Style getStyle()
		{
			return Style.EMPTY;
		}

		@Override
		public TextContent getContent()
		{
			return TextContent.EMPTY;
		}

		static List<Text> emptySiblings = new ArrayList<Text>();

		@Override
		public List<Text> getSiblings()
		{
			return emptySiblings;
		}
	}

	// Bind to text line, because food overlay must apply line offset of all case.
	public static class FoodOverlayTextComponent extends EmptyText implements OrderedText
	{
		public FoodOverlay foodOverlay;

		FoodOverlayTextComponent(FoodOverlay foodOverlay)
		{
			this.foodOverlay = foodOverlay;
		}

		@Override
		public OrderedText asOrderedText()
		{
			return this;
		}

		@Override
		public boolean accept(CharacterVisitor visitor)
		{
			return TextVisitFactory.visitFormatted(this, getStyle(), visitor);
		}
	}

	public static class FoodOverlay implements TooltipComponent, TooltipData
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

		FoodOverlay(ItemStack itemStack, FoodValues defaultFood, FoodValues modifiedFood, PlayerEntity player)
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

		@Override
		public int getHeight()
		{
			// hunger + spacing + saturation + arbitrary spacing,
			// for some reason 3 extra looks best
			return 9 + 1 + 7 + 3;
		}

		@Override
		public int getWidth(TextRenderer textRenderer)
		{
			int hungerBarLength = hungerBars * 9;
			if (hungerBarsText != null)
			{
				hungerBarLength += textRenderer.getWidth(hungerBarsText);
			}
			int saturationBarLength = saturationBars * 7;
			if (saturationBarsText != null)
			{
				saturationBarLength += textRenderer.getWidth(saturationBarsText);
			}
			return Math.max(hungerBarLength, saturationBarLength);
		}

		@Override
		public void drawItems(TextRenderer textRenderer, int x, int y, DrawContext context)
		{
			if (TooltipOverlayHandler.INSTANCE != null)
				TooltipOverlayHandler.INSTANCE.onRenderTooltip(context, this, x, y, 0, textRenderer);
		}
	}

	public static void init()
	{
		INSTANCE = new TooltipOverlayHandler();
	}

	public void onItemTooltip(ItemStack hoveredStack, PlayerEntity player, TooltipContext context, List tooltip)
	{
		// When hoveredStack or tooltip is null an unknown exception occurs.
		// If ModConfig.INSTANCE is null then we're probably still in the init phase
		if (hoveredStack == null || tooltip == null || ModConfig.INSTANCE == null)
			return;

		if (!shouldShowTooltip(hoveredStack))
			return;

		FoodValues defaultFood = FoodHelper.getDefaultFoodValues(hoveredStack);
		FoodValues modifiedFood = FoodHelper.getModifiedFoodValues(hoveredStack, player);

		FoodValuesEvent foodValuesEvent = new FoodValuesEvent(player, hoveredStack, defaultFood, modifiedFood);
		FoodValuesEvent.EVENT.invoker().interact(foodValuesEvent);
		defaultFood = foodValuesEvent.defaultFoodValues;
		modifiedFood = foodValuesEvent.modifiedFoodValues;

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Pre prerenderEvent = new TooltipOverlayEvent.Pre(hoveredStack, defaultFood, modifiedFood);
		TooltipOverlayEvent.Pre.EVENT.invoker().interact(prerenderEvent);
		if (prerenderEvent.isCanceled)
			return;

		FoodOverlay foodOverlay = new FoodOverlay(prerenderEvent.itemStack, defaultFood, modifiedFood, player);
		if (foodOverlay.shouldRenderHungerBars())
		{
			tooltip.add(new FoodOverlayTextComponent(foodOverlay));
		}
	}

	enum FoodOutline
	{
		NEGATIVE,
		EXTRA,
		NORMAL,
		PARTIAL,
		MISSING;

		public void setShaderColor(DrawContext context)
		{
			switch (this)
			{
				case NEGATIVE -> context.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
				case EXTRA -> context.setShaderColor(0.06f, 0.32f, 0.02f, 1.0f);
				case NORMAL -> context.setShaderColor(0.0f, 0.0f, 0.0f, 1.0f);
				case PARTIAL -> context.setShaderColor(0.53f, 0.21f, 0.08f, 1.0f);
				case MISSING -> context.setShaderColor(0.62f, 0.0f, 0.0f, 0.5f);
			}
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

	public void onRenderTooltip(DrawContext context, FoodOverlay foodOverlay, int toolTipX, int toolTipY, int tooltipZ, TextRenderer textRenderer)
	{
		// When matrixStack or tooltip is null an unknown exception occurs.
		// If ModConfig.INSTANCE is null then we're probably still in the init phase
		if (context == null || ModConfig.INSTANCE == null)
			return;

		// Not found overlay text lines, maybe some mods removed it.
		if (foodOverlay == null)
			return;

		MatrixStack matrixStack;
		ItemStack itemStack = foodOverlay.itemStack;

		FoodValues defaultFood = foodOverlay.defaultFood;
		FoodValues modifiedFood = foodOverlay.modifiedFood;

		int x = toolTipX;
		int y = toolTipY;

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Render renderEvent = new TooltipOverlayEvent.Render(itemStack, x, y, context, defaultFood, modifiedFood);
		TooltipOverlayEvent.Render.EVENT.invoker().interact(renderEvent);
		if (renderEvent.isCanceled)
			return;

		x = renderEvent.x;
		y = renderEvent.y;

		context = renderEvent.context;
		itemStack = renderEvent.itemStack;
		matrixStack = context.getMatrices();

		int defaultFoodHunger = defaultFood.hunger;
		int modifiedFoodHunger = modifiedFood.hunger;

		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		// Render from right to left so that the icons 'face' the right way
		x += (foodOverlay.hungerBars - 1) * 9;

		boolean isRotten = FoodHelper.isRotten(itemStack);

		for (int i = 0; i < foodOverlay.hungerBars * 2; i += 2)
		{
			context.drawGuiTexture(TextureHelper.FOOD_EMPTY_TEXTURE, x, y, 9, 9);

			FoodOutline outline = FoodOutline.get(modifiedFoodHunger, defaultFoodHunger, i);
			if (outline != FoodOutline.NORMAL) {
				outline.setShaderColor(context);
				context.drawGuiTexture(TextureHelper.HUNGER_OUTLINE_SPRITE, x, y, 9, 9);
			}

			context.setShaderColor(1.0F, 1.0F, 1.0F, .25F);
			boolean isDefaultHalf = defaultFoodHunger - 1 == i;
			Identifier defaultFoodIcon = TextureHelper.getFoodTexture(isRotten, isDefaultHalf ? FoodType.HALF : FoodType.FULL);
			context.drawGuiTexture(defaultFoodIcon, x, y, 9, 9);
			context.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

			if (modifiedFoodHunger > i)
			{
				boolean isModifiedHalf = modifiedFoodHunger - 1 == i;
				Identifier modifiedFoodIcon = TextureHelper.getFoodTexture(isRotten, isModifiedHalf ? FoodType.HALF : FoodType.FULL);
				context.drawGuiTexture(modifiedFoodIcon, x, y, 9, 9);
			}

			x -= 9;
		}
		if (foodOverlay.hungerBarsText != null)
		{
			x += 18;
			matrixStack.push();
			matrixStack.translate(x, y, tooltipZ);
			matrixStack.scale(0.75f, 0.75f, 0.75f);
			context.drawTextWithShadow(textRenderer, foodOverlay.hungerBarsText, 2, 2, 0xFFAAAAAA);
			matrixStack.pop();
		}

		x = toolTipX;
		y += 10;

		float modifiedSaturationIncrement = modifiedFood.getSaturationIncrement();
		float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

		// Render from right to left so that the icons 'face' the right way
		x += (foodOverlay.saturationBars - 1) * 7;

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		for (int i = 0; i < foodOverlay.saturationBars * 2; i += 2)
		{
			float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

			boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
			if (shouldBeFaded)
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, .5F);

			context.drawTexture(TextureHelper.MOD_ICONS, x, y, tooltipZ, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7, 256, 256);

			if (shouldBeFaded)
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

			x -= 7;
		}
		if (foodOverlay.saturationBarsText != null)
		{
			x += 14;
			matrixStack.push();
			matrixStack.translate(x, y, tooltipZ);
			matrixStack.scale(0.75f, 0.75f, 0.75f);
			context.drawTextWithShadow(textRenderer, foodOverlay.saturationBarsText, 2, 1, 0xFFAAAAAA);
			matrixStack.pop();
		}

		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		// reset to drawHoveringText state
		RenderSystem.disableDepthTest();
	}

	private boolean shouldShowTooltip(ItemStack hoveredStack)
	{
		if (hoveredStack.isEmpty())
		{
			return false;
		}

		boolean shouldShowTooltip = (ModConfig.INSTANCE.showFoodValuesInTooltip && KeyHelper.isShiftKeyDown()) || ModConfig.INSTANCE.showFoodValuesInTooltipAlways;
		if (!shouldShowTooltip)
		{
			return false;
		}

		if (!FoodHelper.isFood(hoveredStack))
		{
			return false;
		}

		return true;
	}
}
