package squeek.appleskin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipData;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.*;
import net.minecraft.util.Identifier;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.TooltipOverlayEvent;
import squeek.appleskin.helpers.ColorHelper;
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
			return PlainTextContent.EMPTY;
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
		private FoodComponent defaultFood;
		private FoodComponent modifiedFood;
		private ConsumableComponent consumableComponent;

		private int biggestHunger;
		private float biggestSaturationIncrement;

		private int hungerBars;
		private String hungerBarsText;

		private int saturationBars;
		private String saturationBarsText;

		private ItemStack itemStack;

		FoodOverlay(ItemStack itemStack, FoodComponent defaultFood, FoodComponent modifiedFood, ConsumableComponent consumableComponent, PlayerEntity player)
		{
			this.itemStack = itemStack;
			this.defaultFood = defaultFood;
			this.modifiedFood = modifiedFood;
			this.consumableComponent = consumableComponent;

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

		@Override
		public int getHeight(TextRenderer textRenderer)
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
		public void drawItems(TextRenderer textRenderer, int x, int y, int width, int height, DrawContext context)
		{
			if (TooltipOverlayHandler.INSTANCE != null)
				TooltipOverlayHandler.INSTANCE.onRenderTooltip(context, this, x, y, 0, textRenderer);
		}
	}

	public static void init()
	{
		INSTANCE = new TooltipOverlayHandler();
	}

	public void onItemTooltip(ItemStack hoveredStack, PlayerEntity player, Item.TooltipContext context, TooltipType type, List tooltip)
	{
		// When hoveredStack or tooltip is null an unknown exception occurs.
		// If ModConfig.INSTANCE is null then we're probably still in the init phase
		if (hoveredStack == null || tooltip == null || ModConfig.INSTANCE == null)
			return;

		if (!shouldShowTooltip(hoveredStack, type))
			return;

		FoodHelper.QueriedFoodResult queriedFoodResult = FoodHelper.query(hoveredStack, player);
		if (queriedFoodResult == null)
			return;

		FoodComponent defaultFood = queriedFoodResult.defaultFoodComponent;
		FoodComponent modifiedFood = queriedFoodResult.modifiedFoodComponent;

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Pre prerenderEvent = new TooltipOverlayEvent.Pre(hoveredStack, defaultFood, modifiedFood);
		TooltipOverlayEvent.Pre.EVENT.invoker().interact(prerenderEvent);
		if (prerenderEvent.isCanceled)
			return;

		FoodOverlay foodOverlay = new FoodOverlay(prerenderEvent.itemStack, defaultFood, modifiedFood, queriedFoodResult.consumableComponent, player);
		if (foodOverlay.shouldRenderHungerBars())
		{
			try
			{
				tooltip.add(new FoodOverlayTextComponent(foodOverlay));
			}
			catch (UnsupportedOperationException ignored)
			{
				// The list is immutable, e.g. the item has the HIDE_TOOLTIP component.
				// In addition to checking for that component, we catch this exception
				// just in case there are other reasons the list could be immutable.
			}
		}
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

		FoodComponent defaultFood = foodOverlay.defaultFood;
		FoodComponent modifiedFood = foodOverlay.modifiedFood;

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

		int defaultFoodHunger = defaultFood.nutrition();
		int modifiedFoodHunger = modifiedFood.nutrition();

		// Render from right to left so that the icons 'face' the right way
		x += (foodOverlay.hungerBars - 1) * 9;

		boolean isRotten = FoodHelper.isRotten(foodOverlay.consumableComponent);

		for (int i = 0; i < foodOverlay.hungerBars * 2; i += 2)
		{
			context.drawGuiTexture(RenderLayer::getGuiTextured, TextureHelper.FOOD_EMPTY_TEXTURE, x, y, 9, 9);

			FoodOutline outline = FoodOutline.get(modifiedFoodHunger, defaultFoodHunger, i);
			if (outline != FoodOutline.NORMAL)
			{
				context.drawGuiTexture(RenderLayer::getGuiTextured, TextureHelper.HUNGER_OUTLINE_SPRITE, x, y, 9, 9, outline.argb());
			}

			boolean isDefaultHalf = defaultFoodHunger - 1 == i;
			Identifier defaultFoodIcon = TextureHelper.getFoodTexture(isRotten, isDefaultHalf ? FoodType.HALF : FoodType.FULL);
			context.drawGuiTexture(RenderLayer::getGuiTextured, defaultFoodIcon, x, y, 9, 9, ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.25F));

			if (modifiedFoodHunger > i)
			{
				boolean isModifiedHalf = modifiedFoodHunger - 1 == i;
				Identifier modifiedFoodIcon = TextureHelper.getFoodTexture(isRotten, isModifiedHalf ? FoodType.HALF : FoodType.FULL);
				context.drawGuiTexture(RenderLayer::getGuiTextured, modifiedFoodIcon, x, y, 9, 9);
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

		float modifiedSaturationIncrement = modifiedFood.saturation();
		float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

		// Render from right to left so that the icons 'face' the right way
		x += (foodOverlay.saturationBars - 1) * 7;

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		for (int i = 0; i < foodOverlay.saturationBars * 2; i += 2)
		{
			float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

			boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
			int color = shouldBeFaded ? ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.5F) : ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 1.0F);
			context.drawTexture(RenderLayer::getGuiTextured, TextureHelper.MOD_ICONS, x, y, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7, 256, 256, color);

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
	}

	private boolean shouldShowTooltip(ItemStack hoveredStack, TooltipType type)
	{
		if (hoveredStack.isEmpty())
		{
			return false;
		}

		// Note: The intention here is to match the logic in ItemStack.getTooltip
		if (!type.isCreative() &&
				(hoveredStack.contains(DataComponentTypes.TOOLTIP_DISPLAY) && hoveredStack.get(DataComponentTypes.TOOLTIP_DISPLAY).hideTooltip()))
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
