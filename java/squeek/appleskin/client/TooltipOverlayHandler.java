package squeek.appleskin.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Identifier;
import squeek.appleskin.AppleSkin;
import squeek.appleskin.event.TooltipOverlayEvent;
import squeek.appleskin.helpers.FoodHelper;

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
	static class FoodOverlayTextComponent extends LiteralText
	{
		private FoodOverlay foodOverlay;
		FoodOverlayTextComponent(FoodOverlay foodOverlay)
		{
			super(foodOverlay.getTooltip());
			this.foodOverlay = foodOverlay;
		}

		public FoodOverlayTextComponent copy() {
			return new FoodOverlayTextComponent(foodOverlay);
		}
	}

	static class FoodOverlay
	{
		private FoodHelper.BasicFoodValues defaultFoodValues;
		private FoodHelper.BasicFoodValues modifiedFoodValues;

		private int biggestHunger;
		private float biggestSaturationIncrement;

		private int hungerBars;
		private String hungerBarsText;

		private int saturationBars;
		private String saturationBarsText;

		private String tooltip;

		private ItemStack itemStack;

		FoodOverlay(ItemStack itemStack, FoodHelper.BasicFoodValues defaultFoodValues, FoodHelper.BasicFoodValues modifiedFoodValues)
		{
			this.itemStack = itemStack;
			this.defaultFoodValues = defaultFoodValues;
			this.modifiedFoodValues = modifiedFoodValues;

			biggestHunger = Math.max(defaultFoodValues.hunger, modifiedFoodValues.hunger);
			biggestSaturationIncrement = Math.max(defaultFoodValues.getSaturationIncrement(), modifiedFoodValues.getSaturationIncrement());

			hungerBars = (int) Math.ceil(Math.abs(biggestHunger) / 2f);
			if (hungerBars > 10) {
				hungerBarsText = "x" + ((biggestHunger < 0 ? -1 : 1) * hungerBars);
				hungerBars = 1;
			}

			saturationBars = (int) Math.max(1, Math.ceil(Math.abs(biggestSaturationIncrement) / 2f));
			if (saturationBars > 10) {
				saturationBarsText = "x" + ((biggestSaturationIncrement < 0 ? -1 : 1) * saturationBars);
				saturationBars = 1;
			}
		}

		String getTooltip()
		{
			if (tooltip != null) {
				return tooltip;
			}
			// 9x9 icon convert to scale of blank string.
			float scale = 2.2f;

			float hungerBarsLength = (float) hungerBars * scale;
			if (hungerBarsText != null) {
				hungerBarsLength += hungerBarsText.length();
			}

			float saturationBarsLength = (float) saturationBars * scale;
			if (saturationBarsText != null) {
				saturationBarsLength += saturationBarsText.length();
			}

			int length = (int) Math.ceil(Math.max(hungerBarsLength, saturationBarsLength * 0.8f));
			StringBuilder s = new StringBuilder(" ");
			for (int i = 0; i < length; i++) {
				s.append(" ");
			}

			tooltip = s.toString();
			return tooltip;
		}

		boolean shouldRenderHungerBars()
		{
			return hungerBars > 0;
		}

		boolean shouldRenderSaturationBars()
		{
			return saturationBars > 0;
		}
	}
	
	public static void onItemTooltip(ItemStack hoveredStack, List tooltip)
	{
		// When hoveredStack or tooltip is null an unknown exception occurs.
		if (hoveredStack == null || tooltip == null) {
			return;
		}
		if (!shouldShowTooltip(hoveredStack)) {
			return;
		}

		MinecraftClient mc = MinecraftClient.getInstance();

		FoodHelper.BasicFoodValues defaultFoodValues = FoodHelper.getDefaultFoodValues(hoveredStack);
		FoodHelper.BasicFoodValues modifiedFoodValues = FoodHelper.getModifiedFoodValues(hoveredStack, mc.player);

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Pre prerenderEvent = new TooltipOverlayEvent.Pre(hoveredStack, defaultFoodValues, modifiedFoodValues);
		AppleSkin.EVENT_BUS.post(prerenderEvent);
		if (prerenderEvent.isCanceled) {
			return;
		}

		FoodOverlay foodOverlay = new FoodOverlay(prerenderEvent.itemStack, prerenderEvent.defaultFoodValues, prerenderEvent.modifiedFoodValues);
		if (foodOverlay.shouldRenderHungerBars()) {
			tooltip.add(new FoodOverlayTextComponent(foodOverlay));
		}
		if (foodOverlay.shouldRenderSaturationBars()) {
			tooltip.add(new FoodOverlayTextComponent(foodOverlay));
		}
	}

	public static void onRenderTooltip(MatrixStack matrixStack, List tooltip, int toolTipX, int toolTipY, int toolTipW, int toolTipH)
	{
		// When matrixStack or tooltip is null an unknown exception occurs.
		if (matrixStack == null || tooltip == null) {
			return;
		}

		MinecraftClient mc = MinecraftClient.getInstance();
		Screen gui = mc.currentScreen;
		if (gui == null) {
			return;
		}

		// Find food overlay of text lines.
		FoodOverlay foodOverlay = null;
		for (int i = 0; i < tooltip.size(); ++i) {
			if (tooltip.get(i) instanceof FoodOverlayTextComponent) {
				toolTipY += i * 10;
				foodOverlay = ((FoodOverlayTextComponent)tooltip.get(i)).foodOverlay;
				break;
			}
		}

		// Not found overlay text lines, maybe some mods removed it.
		if (foodOverlay == null) {
			return;
		}

		FoodHelper.BasicFoodValues defaultFoodValues = foodOverlay.defaultFoodValues;
		FoodHelper.BasicFoodValues modifiedFoodValues = foodOverlay.modifiedFoodValues;

		int x = toolTipX;
		int y = toolTipY + 2;

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Post renderEvent = new TooltipOverlayEvent.Post(foodOverlay.itemStack, x, y, matrixStack, defaultFoodValues, modifiedFoodValues);
		AppleSkin.EVENT_BUS.post(renderEvent);
		if (renderEvent.isCanceled) {
			return;
		}

		x = renderEvent.x;
		y = renderEvent.y;
		matrixStack = renderEvent.matrixStack;
		defaultFoodValues = renderEvent.defaultFoodValues;
		modifiedFoodValues = renderEvent.modifiedFoodValues;

		RenderSystem.disableLighting();
		RenderSystem.enableDepthTest();

		matrixStack.push();
		matrixStack.translate(0.0D, 0.0D, 500D); // zLevel must higher than of the background.

		mc.getTextureManager().bindTexture(Screen.GUI_ICONS_TEXTURE);
		TextureOffsets offsets = FoodHelper.isRotten(foodOverlay.itemStack) ? rottenBarTextureOffsets : normalBarTextureOffsets;
		for (int i = 0; i < foodOverlay.hungerBars * 2; i += 2) {

			if (modifiedFoodValues.hunger < 0)
				gui.drawTexture(matrixStack, x, y, offsets.containerNegativeHunger, 27, 9, 9);
			else if (modifiedFoodValues.hunger > defaultFoodValues.hunger && defaultFoodValues.hunger <= i)
				gui.drawTexture(matrixStack, x, y, offsets.containerExtraHunger, 27, 9, 9);
			else if (modifiedFoodValues.hunger > i + 1 || defaultFoodValues.hunger == modifiedFoodValues.hunger)
				gui.drawTexture(matrixStack, x, y, offsets.containerNormalHunger, 27, 9, 9);
			else if (modifiedFoodValues.hunger == i + 1)
				gui.drawTexture(matrixStack, x, y, offsets.containerPartialHunger, 27, 9, 9);
			else
				gui.drawTexture(matrixStack, x, y, offsets.containerMissingHunger, 27, 9, 9);

			RenderSystem.color4f(1.0F, 1.0F, 1.0F, .25F);
			gui.drawTexture(matrixStack, x, y, defaultFoodValues.hunger - 1 == i ? offsets.shankMissingPartial : offsets.shankMissingFull, 27, 9, 9);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

			if (modifiedFoodValues.hunger > i)
				gui.drawTexture(matrixStack, x, y, modifiedFoodValues.hunger - 1 == i ? offsets.shankPartial : offsets.shankFull, 27, 9, 9);

			x += 9;
		}
		if (foodOverlay.hungerBarsText != null) {
			matrixStack.push();
			matrixStack.translate(x, y, 0);
			matrixStack.scale(0.75f, 0.75f, 0.75f);
			mc.textRenderer.drawWithShadow(matrixStack, foodOverlay.hungerBarsText, 2, 2, 0xFFDDDDDD);
			matrixStack.pop();
		}

		x = toolTipX;
		y += 10;

		float modifiedSaturationIncrement = modifiedFoodValues.getSaturationIncrement();
		float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(modIcons);
		for (int i = 0; i < foodOverlay.saturationBars * 2; i += 2) {
			float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

			boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
			if (shouldBeFaded)
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, .5F);

			gui.drawTexture(matrixStack, x, y, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7);

			if (shouldBeFaded)
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

			x += 7;
		}
		if (foodOverlay.saturationBarsText != null) {
			matrixStack.push();
			matrixStack.translate(x, y, 0);
			matrixStack.scale(0.75f, 0.75f, 0.75f);
			mc.textRenderer.drawWithShadow(matrixStack, foodOverlay.saturationBarsText, 2, 1, 0xFFDDDDDD);
			matrixStack.pop();
		}

		matrixStack.pop();

		RenderSystem.disableBlend();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		// reset to drawHoveringText state
		RenderSystem.disableRescaleNormal();
		RenderSystem.disableLighting();
		RenderSystem.disableDepthTest();
	}

	private static boolean shouldShowTooltip(ItemStack hoveredStack)
	{
		if (hoveredStack.isEmpty()) {
			return false;
		}

		if (!FoodHelper.isFood(hoveredStack)) {
			return false;
		}

		return true;
	}
}
