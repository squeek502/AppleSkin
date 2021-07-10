package squeek.appleskin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextVisitFactory;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.TooltipComponent;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.CharacterVisitor;
import net.minecraft.text.LiteralText;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import squeek.appleskin.ModConfig;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.event.TooltipOverlayEvent;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.KeyHelper;

import java.util.List;

public class TooltipOverlayHandler
{
	private static Identifier modIcons = new Identifier("appleskin", "textures/icons.png");
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_BOTTOM = 3;
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_TOP = -3;
	public static final int TOOLTIP_REAL_WIDTH_OFFSET_RIGHT = 3;

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

	// Bind to text line, because food overlay must apply line offset of all case.
	public static class FoodOverlayTextComponent extends LiteralText implements OrderedText
	{
		public FoodOverlay foodOverlay;

		FoodOverlayTextComponent(FoodOverlay foodOverlay)
		{
			super("");
			this.foodOverlay = foodOverlay;
		}

		public FoodOverlayTextComponent copy()
		{
			return new FoodOverlayTextComponent(foodOverlay);
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

	static class FoodOverlay implements TooltipComponent
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
			if (hungerBarsText != null) {
				hungerBarLength += textRenderer.getWidth(hungerBarsText);
			}
			int saturationBarLength = saturationBars * 7;
			if (saturationBarsText != null) {
				saturationBarLength += textRenderer.getWidth(saturationBarsText);
			}
			return Math.max(hungerBarLength, saturationBarLength);
		}

		@Override
		public void drawItems(TextRenderer textRenderer, int x, int y, MatrixStack matrices, ItemRenderer itemRenderer, int z, TextureManager textureManager)
		{
			TooltipOverlayHandler.onRenderTooltip(matrices, this, x, y, z, textRenderer);
		}
	}

	public static void onItemTooltip(ItemStack hoveredStack, PlayerEntity player, TooltipContext context, List tooltip)
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

	public static void onRenderTooltip(MatrixStack matrixStack, FoodOverlay foodOverlay, int toolTipX, int toolTipY, int tooltipZ, TextRenderer textRenderer)
	{
		// When matrixStack or tooltip is null an unknown exception occurs.
		// If ModConfig.INSTANCE is null then we're probably still in the init phase
		if (matrixStack == null || ModConfig.INSTANCE == null)
			return;

		// Not found overlay text lines, maybe some mods removed it.
		if (foodOverlay == null)
			return;

		ItemStack itemStack = foodOverlay.itemStack;

		FoodValues defaultFood = foodOverlay.defaultFood;
		FoodValues modifiedFood = foodOverlay.modifiedFood;

		int x = toolTipX;
		int y = toolTipY;

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Render renderEvent = new TooltipOverlayEvent.Render(itemStack, x, y, matrixStack, defaultFood, modifiedFood);
		TooltipOverlayEvent.Render.EVENT.invoker().interact(renderEvent);
		if (renderEvent.isCanceled)
			return;

		x = renderEvent.x;
		y = renderEvent.y;

		itemStack = renderEvent.itemStack;
		matrixStack = renderEvent.matrixStack;

		int defaultFoodHunger = defaultFood.hunger;
		int modifiedFoodHunger = modifiedFood.hunger;

		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		// Render from right to left so that the icons 'face' the right way
		x += (foodOverlay.hungerBars - 1) * 9;

		RenderSystem.setShaderTexture(0, Screen.GUI_ICONS_TEXTURE);
		TextureOffsets offsets = FoodHelper.isRotten(itemStack) ? rottenBarTextureOffsets : normalBarTextureOffsets;
		for (int i = 0; i < foodOverlay.hungerBars * 2; i += 2)
		{

			if (modifiedFoodHunger < 0)
				DrawableHelper.drawTexture(matrixStack, x, y, tooltipZ, offsets.containerNegativeHunger, 27, 9, 9, 256, 256);
			else if (modifiedFoodHunger > defaultFoodHunger && defaultFoodHunger <= i)
				DrawableHelper.drawTexture(matrixStack, x, y, tooltipZ, offsets.containerExtraHunger, 27, 9, 9, 256, 256);
			else if (modifiedFoodHunger > i + 1 || defaultFoodHunger == modifiedFoodHunger)
				DrawableHelper.drawTexture(matrixStack, x, y, tooltipZ, offsets.containerNormalHunger, 27, 9, 9, 256, 256);
			else if (modifiedFoodHunger == i + 1)
				DrawableHelper.drawTexture(matrixStack, x, y, tooltipZ, offsets.containerPartialHunger, 27, 9, 9, 256, 256);
			else
			{
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, .5F);
				DrawableHelper.drawTexture(matrixStack, x, y, tooltipZ, offsets.containerMissingHunger, 27, 9, 9, 256, 256);
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			}

			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, .25F);
			DrawableHelper.drawTexture(matrixStack, x, y, tooltipZ, defaultFoodHunger - 1 == i ? offsets.shankMissingPartial : offsets.shankMissingFull, 27, 9, 9, 256, 256);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

			if (modifiedFoodHunger > i)
				DrawableHelper.drawTexture(matrixStack, x, y, tooltipZ, modifiedFoodHunger - 1 == i ? offsets.shankPartial : offsets.shankFull, 27, 9, 9, 256, 256);

			x -= 9;
		}
		if (foodOverlay.hungerBarsText != null)
		{
			x += 18;
			matrixStack.push();
			matrixStack.translate(x, y, tooltipZ);
			matrixStack.scale(0.75f, 0.75f, 0.75f);
			textRenderer.drawWithShadow(matrixStack, foodOverlay.hungerBarsText, 2, 2, 0xFFDDDDDD);
			matrixStack.pop();
		}

		x = toolTipX;
		y += 10;

		float modifiedSaturationIncrement = modifiedFood.getSaturationIncrement();
		float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

		// Render from right to left so that the icons 'face' the right way
		x += (foodOverlay.saturationBars - 1) * 7;

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		RenderSystem.setShaderTexture(0, modIcons);
		for (int i = 0; i < foodOverlay.saturationBars * 2; i += 2)
		{
			float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

			boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
			if (shouldBeFaded)
				RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, .5F);

			DrawableHelper.drawTexture(matrixStack, x, y, tooltipZ, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7, 256, 256);

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
			textRenderer.drawWithShadow(matrixStack, foodOverlay.saturationBarsText, 2, 1, 0xFFDDDDDD);
			matrixStack.pop();
		}

		RenderSystem.disableBlend();
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		// reset to drawHoveringText state
		RenderSystem.disableDepthTest();
	}

	private static boolean shouldShowTooltip(ItemStack hoveredStack)
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
