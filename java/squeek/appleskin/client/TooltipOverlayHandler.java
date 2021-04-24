package squeek.appleskin.client;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.opengl.GL11;
import squeek.appleskin.ModConfig;
import squeek.appleskin.ModInfo;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.KeyHelper;

import java.util.List;

@OnlyIn(Dist.CLIENT)
public class TooltipOverlayHandler
{
	private static ResourceLocation modIcons = new ResourceLocation(ModInfo.MODID_LOWER, "textures/icons.png");
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_BOTTOM = 3;
	public static final int TOOLTIP_REAL_HEIGHT_OFFSET_TOP = -3;
	public static final int TOOLTIP_REAL_WIDTH_OFFSET_RIGHT = 3;

	public static void init()
	{
		MinecraftForge.EVENT_BUS.register(new TooltipOverlayHandler());
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

	// Bind to text line, because food overlay must apply line offset of all case.
	static class FoodOverlayTextComponent extends StringTextComponent {

		private FoodOverlay foodOverlay;
		FoodOverlayTextComponent(FoodOverlay foodOverlay) {
			super(foodOverlay.getTooltip());
			this.foodOverlay = foodOverlay;
		}

		public FoodOverlayTextComponent copyRaw() {
			return new FoodOverlayTextComponent(foodOverlay);
		}
	}

	static class FoodOverlay {

		private FoodHelper.BasicFoodValues defaultFoodValues;
		private FoodHelper.BasicFoodValues modifiedFoodValues;

		private int biggestHunger;
		private float biggestSaturationIncrement;

		private int hungerBars;
		private String hungerBarsText;

		private int saturationBars;
		private String saturationBarsText;

		private String tooltip;

		FoodOverlay(ItemStack itemStack, PlayerEntity player) {

			defaultFoodValues = FoodHelper.getDefaultFoodValues(itemStack);
			modifiedFoodValues = FoodHelper.getModifiedFoodValues(itemStack, player);

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

			tooltip = getPlaceholder();
		}

		String getTooltip() {
			return tooltip;
		}
		String getPlaceholder() {
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

			return s.toString();
		}

		boolean shouldRenderHungerBars() {
			return hungerBars > 0;
		}

		boolean shouldRenderSaturationBars() {
			return saturationBars > 0;
		}
	}

	@SubscribeEvent
	public void onItemTooltip(ItemTooltipEvent event)
	{
		if (event.isCanceled()) {
			return;
		}

		ItemStack hoveredStack = event.getItemStack();
		if (!shouldShowTooltip(hoveredStack)) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		PlayerEntity player = mc.player;
		List<ITextComponent> tooltip = event.getToolTip();
		if (tooltip == null) {
			return;
		}

		FoodOverlay foodOverlay = new FoodOverlay(hoveredStack, player);
		if (foodOverlay.shouldRenderHungerBars()) {
			tooltip.add(new FoodOverlayTextComponent(foodOverlay));
		}
		if (foodOverlay.shouldRenderSaturationBars()) {
			tooltip.add(new FoodOverlayTextComponent(foodOverlay));
		}
	}

	@SubscribeEvent
	public void onRenderTooltip(RenderTooltipEvent.PostText event)
	{
		if (event.isCanceled()) {
			return;
		}

		ItemStack hoveredStack = event.getStack();
		if (!shouldShowTooltip(hoveredStack)) {
			return;
		}

		Minecraft mc = Minecraft.getInstance();
		Screen gui = mc.currentScreen;
		if (gui == null) {
			return;
		}

		int toolTipY = event.getY();
		int toolTipX = event.getX();
		int toolTipZ = 400; // tooltip text zLevel is 400, hardcode in GuiUtils.

		// Find food overlay of text lines.
		FoodOverlay foodOverlay = null;
		List<? extends ITextProperties> lines = event.getLines();
		for (int i = 0; i < lines.size(); ++i) {
			ITextProperties line = lines.get(i);
			if (line instanceof FoodOverlayTextComponent) {
				toolTipY += i * 10;
				foodOverlay = ((FoodOverlayTextComponent)line).foodOverlay;
				break;
			}
		}

		// Not found overlay text lines, maybe some mods removed it.
		if (foodOverlay == null) {
			return;
		}

		FoodHelper.BasicFoodValues defaultFoodValues = foodOverlay.defaultFoodValues;
		FoodHelper.BasicFoodValues modifiedFoodValues = foodOverlay.modifiedFoodValues;

		MatrixStack matrixStack = event.getMatrixStack();

		matrixStack.push();
		matrixStack.translate(0.0D, 0.0D, toolTipZ);

		RenderSystem.disableLighting();
		RenderSystem.disableDepthTest();

		int x = toolTipX;
		int y = toolTipY + 2;

		mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
		TextureOffsets offsets = FoodHelper.isRotten(hoveredStack) ? rottenBarTextureOffsets : normalBarTextureOffsets;
		for (int i = 0; i < foodOverlay.hungerBars * 2; i += 2) {

			if (modifiedFoodValues.hunger < 0)
				gui.blit(matrixStack, x, y, offsets.containerNegativeHunger, 27, 9, 9);
			else if (modifiedFoodValues.hunger > defaultFoodValues.hunger && defaultFoodValues.hunger <= i)
				gui.blit(matrixStack, x, y, offsets.containerExtraHunger, 27, 9, 9);
			else if (modifiedFoodValues.hunger > i + 1 || defaultFoodValues.hunger == modifiedFoodValues.hunger)
				gui.blit(matrixStack, x, y, offsets.containerNormalHunger, 27, 9, 9);
			else if (modifiedFoodValues.hunger == i + 1)
				gui.blit(matrixStack, x, y, offsets.containerPartialHunger, 27, 9, 9);
			else
				gui.blit(matrixStack, x, y, offsets.containerMissingHunger, 27, 9, 9);

			RenderSystem.color4f(1.0F, 1.0F, 1.0F, .25F);
			gui.blit(matrixStack, x, y, defaultFoodValues.hunger - 1 == i ? offsets.shankMissingPartial : offsets.shankMissingFull, 27, 9, 9);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

			if (modifiedFoodValues.hunger > i)
				gui.blit(matrixStack, x, y, modifiedFoodValues.hunger - 1 == i ? offsets.shankPartial : offsets.shankFull, 27, 9, 9);

			x += 9;
		}
		if (foodOverlay.hungerBarsText != null) {
			matrixStack.push();
			matrixStack.translate(x, y, 0);
			matrixStack.scale(0.75f, 0.75f, 0.75f);
			mc.fontRenderer.func_238406_a_(matrixStack, foodOverlay.hungerBarsText, 2, 2, 0xFFDDDDDD, false);
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

			gui.blit(matrixStack, x, y, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7);

			if (shouldBeFaded)
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

			x += 7;
		}
		if (foodOverlay.saturationBarsText != null) {
			matrixStack.push();
			matrixStack.translate(x, y, 0);
			matrixStack.scale(0.75f, 0.75f, 0.75f);
			mc.fontRenderer.func_238406_a_(matrixStack, foodOverlay.saturationBarsText, 2, 1, 0xFFDDDDDD, false);
			matrixStack.pop();
		}

		matrixStack.pop();

		RenderSystem.disableBlend();
		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

		// reset to drawHoveringText state
		RenderSystem.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
		RenderSystem.disableLighting();
		RenderSystem.disableDepthTest();
	}

	private boolean shouldShowTooltip(ItemStack hoveredStack) {
		if (hoveredStack.isEmpty()) {
			return false;
		}

		boolean shouldShowTooltip = (ModConfig.SHOW_FOOD_VALUES_IN_TOOLTIP.get() && KeyHelper.isShiftKeyDown()) || ModConfig.ALWAYS_SHOW_FOOD_VALUES_TOOLTIP.get();
		if (!shouldShowTooltip) {
			return false;
		}

		if (!FoodHelper.isFood(hoveredStack)) {
			return false;
		}

		return true;
	}
}
