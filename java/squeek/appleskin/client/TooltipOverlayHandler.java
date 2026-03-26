package squeek.appleskin.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.FormattedCharSink;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2fStack;
import org.jspecify.annotations.NonNull;
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

	// 26.1: Text was RUTHLESSLY shoved into Component...
	static abstract class EmptyText implements Component
	{
		@Override
		public @NonNull Style getStyle()
		{
			return Style.EMPTY;
		}

		@Override
		public @NonNull ComponentContents getContents()
		{
			return PlainTextContents.EMPTY;
		}

		private static final List<Component> emptySiblings = new ArrayList<>();

		@Override
		public @NonNull List<Component> getSiblings()
		{
			return emptySiblings;
		}
	}

	// Bind to text line, because food overlay must apply line offset of all case.
	public static class FoodOverlayTextComponent extends EmptyText implements FormattedCharSequence
	{
		public final FoodOverlay foodOverlay;

		FoodOverlayTextComponent(FoodOverlay foodOverlay)
		{
			this.foodOverlay = foodOverlay;
		}

		@Override
		public @NonNull FormattedCharSequence getVisualOrderText()
		{
			return this;
		}

		@Override
		public boolean accept(@NonNull FormattedCharSink output)
		{
			return StringDecomposer.iterateFormatted(this, getStyle(), output);
		}
	}

	public static class FoodOverlay implements ClientTooltipComponent, TooltipComponent
	{
		private final FoodProperties defaultFood;
		private final FoodProperties modifiedFood;
		private final Consumable consumableComponent;

		private int hungerBars;
		private String hungerBarsText;

		private int saturationBars;
		private String saturationBarsText;

		private final ItemStack itemStack;

		FoodOverlay(ItemStack itemStack, FoodProperties defaultFood, FoodProperties modifiedFood, Consumable consumableComponent)
		{
			this.itemStack = itemStack;
			this.defaultFood = defaultFood;
			this.modifiedFood = modifiedFood;
			this.consumableComponent = consumableComponent;

			int biggestHunger = Math.max(defaultFood.nutrition(), modifiedFood.nutrition());
			float biggestSaturationIncrement = Math.max(defaultFood.saturation(), modifiedFood.saturation());

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
		public int getHeight(@NonNull Font textRenderer)
		{
			// hunger + spacing + saturation + arbitrary spacing,
			// for some reason 3 extra looks best
			return 9 + 1 + 7 + 3;
		}

		@Override
		public int getWidth(@NonNull Font textRenderer)
		{
			int hungerBarLength = hungerBars * 9;
			if (hungerBarsText != null)
			{
				hungerBarLength += textRenderer.width(hungerBarsText);
			}
			int saturationBarLength = saturationBars * 7;
			if (saturationBarsText != null)
			{
				saturationBarLength += textRenderer.width(saturationBarsText);
			}
			return Math.max(hungerBarLength, saturationBarLength);
		}

		public void extractImage(@NonNull Font textRenderer, int x, int y, int width, int height, @NonNull GuiGraphicsExtractor graphics)
		{
			if (TooltipOverlayHandler.INSTANCE != null)
				TooltipOverlayHandler.INSTANCE.onRenderTooltip(graphics, this, x, y, textRenderer);
		}
	}

	public static void init()
	{
		INSTANCE = new TooltipOverlayHandler();
	}

	public void onItemTooltip(ItemStack hoveredStack, Player player, TooltipFlag type, List<Component> tooltip)
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

		FoodProperties defaultFood = queriedFoodResult.defaultFoodComponent;
		FoodProperties modifiedFood = queriedFoodResult.modifiedFoodComponent;

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Pre prerenderEvent = new TooltipOverlayEvent.Pre(hoveredStack, defaultFood, modifiedFood);
		TooltipOverlayEvent.Pre.EVENT.invoker().interact(prerenderEvent);
		if (prerenderEvent.isCanceled)
			return;

		FoodOverlay foodOverlay = new FoodOverlay(prerenderEvent.itemStack, defaultFood, modifiedFood, queriedFoodResult.consumableComponent);
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

	public void onRenderTooltip(GuiGraphicsExtractor graphics, FoodOverlay foodOverlay, int toolTipX, int toolTipY, Font textRenderer)
	{
		// When matrixStack or tooltip is null an unknown exception occurs.
		// If ModConfig.INSTANCE is null then we're probably still in the init phase
		if (graphics == null || ModConfig.INSTANCE == null)
			return;

		// Not found overlay text lines, maybe some mods removed it.
		if (foodOverlay == null)
			return;

		Matrix3x2fStack matrixStack;
		ItemStack itemStack = foodOverlay.itemStack;

		FoodProperties defaultFood = foodOverlay.defaultFood;
		FoodProperties modifiedFood = foodOverlay.modifiedFood;

		int x = toolTipX;
		int y = toolTipY;

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Render renderEvent = new TooltipOverlayEvent.Render(itemStack, x, y, graphics, defaultFood, modifiedFood);
		TooltipOverlayEvent.Render.EVENT.invoker().interact(renderEvent);
		if (renderEvent.isCanceled)
			return;

		x = renderEvent.x;
		y = renderEvent.y;

		graphics = renderEvent.graphics;
		matrixStack = graphics.pose();

		int defaultFoodHunger = defaultFood.nutrition();
		int modifiedFoodHunger = modifiedFood.nutrition();

		// Render from right to left so that the icons 'face' the right way
		x += (foodOverlay.hungerBars - 1) * 9;

		boolean isRotten = FoodHelper.isRotten(foodOverlay.consumableComponent);

		for (int i = 0; i < foodOverlay.hungerBars * 2; i += 2)
		{
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.FOOD_EMPTY_TEXTURE, x, y, 9, 9);

			FoodOutline outline = FoodOutline.get(modifiedFoodHunger, defaultFoodHunger, i);
			if (outline != FoodOutline.NORMAL)
			{
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, TextureHelper.HUNGER_OUTLINE_SPRITE, x, y, 9, 9, outline.argb());
			}

			boolean isDefaultHalf = defaultFoodHunger - 1 == i;
			Identifier defaultFoodIcon = TextureHelper.getFoodTexture(isRotten, isDefaultHalf ? FoodType.HALF : FoodType.FULL);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, defaultFoodIcon, x, y, 9, 9, ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.25F));

			if (modifiedFoodHunger > i)
			{
				boolean isModifiedHalf = modifiedFoodHunger - 1 == i;
				Identifier modifiedFoodIcon = TextureHelper.getFoodTexture(isRotten, isModifiedHalf ? FoodType.HALF : FoodType.FULL);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, modifiedFoodIcon, x, y, 9, 9);
			}

			x -= 9;
		}
		if (foodOverlay.hungerBarsText != null)
		{
			x += 18;
			matrixStack.pushMatrix();
			matrixStack.translate(x, y);
			matrixStack.scale(0.75f, 0.75f);
			graphics.text(textRenderer, foodOverlay.hungerBarsText, 2, 2, 0xFFAAAAAA);
			matrixStack.popMatrix();
		}

		x = toolTipX;
		y += 10;

		float modifiedSaturationIncrement = modifiedFood.saturation();
		float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

		// Render from right to left so that the icons 'face' the right way
		x += (foodOverlay.saturationBars - 1) * 7;

		for (int i = 0; i < foodOverlay.saturationBars * 2; i += 2)
		{
			float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

			boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
			int color = shouldBeFaded ? ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 0.5F) : ColorHelper.argbFromRGBA(1.0F, 1.0F, 1.0F, 1.0F);
			graphics.blit(RenderPipelines.GUI_TEXTURED, TextureHelper.MOD_ICONS, x, y, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7, 256, 256, color);

			x -= 7;
		}
		if (foodOverlay.saturationBarsText != null)
		{
			x += 14;
			matrixStack.pushMatrix();
			matrixStack.translate(x, y);
			matrixStack.scale(0.75f, 0.75f);
			graphics.text(textRenderer, foodOverlay.saturationBarsText, 2, 1, 0xFFAAAAAA);
			matrixStack.popMatrix();
		}
	}

	private boolean shouldShowTooltip(ItemStack hoveredStack, TooltipFlag type)
	{
		if (hoveredStack.isEmpty())
		{
			return false;
		}

		// Note: The intention here is to match the logic in ItemStack.getTooltip
		if (!type.isCreative() && hoveredStack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT).hideTooltip())
		{
			return false;
		}

		boolean shouldShowTooltip = (ModConfig.INSTANCE.showFoodValuesInTooltip && KeyHelper.isShiftKeyDown()) || ModConfig.INSTANCE.showFoodValuesInTooltipAlways;
		if (!shouldShowTooltip)
		{
			return false;
		}

		return !FoodHelper.isNotFood(hoveredStack);
	}
}
