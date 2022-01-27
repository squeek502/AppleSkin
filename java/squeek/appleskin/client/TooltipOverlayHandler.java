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
import net.minecraft.util.text.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import squeek.appleskin.ModConfig;
import squeek.appleskin.ModInfo;
import squeek.appleskin.api.event.FoodValuesEvent;
import squeek.appleskin.api.event.TooltipOverlayEvent;
import squeek.appleskin.api.food.FoodValues;
import squeek.appleskin.helpers.FoodHelper;
import squeek.appleskin.helpers.KeyHelper;

import java.util.List;
import java.util.Optional;

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

	// We set a special font into placeholder text, so font will bind a food overlay,
	// When the placeholder text is convert or truncation, user will restore the style.
	static class FoodOverlayFont extends ResourceLocation
	{
		private FoodOverlay foodOverlay;
		FoodOverlayFont(FoodOverlay foodOverlay)
		{
			super(Style.DEFAULT_FONT.getNamespace(), Style.DEFAULT_FONT.getPath());
			this.foodOverlay = foodOverlay;
		}

		static Object getFontId(ITextProperties line)
		{
			// A fast path, however not all lines conform `ITextComponent`.
			if (line instanceof ITextComponent)
				return ((ITextComponent)line).getStyle().getFontId();

			// A slow path, only to check frist string.
			final Object[] fontId = { Style.DEFAULT_FONT };
			line.getComponentWithStyle(new ITextProperties.IStyledTextAcceptor<ITextProperties>() {
				public Optional<ITextProperties> accept(Style n, String s) {
					fontId[0] = n.getFontId();
					return Optional.empty();
				}
			}, Style.EMPTY);
			return fontId[0];
		}

		static FoodOverlay getFoodOverlay(ITextProperties line)
		{
			Object fontId = getFontId(line);
			if (fontId instanceof FoodOverlayFont)
				return ((FoodOverlayFont) fontId).foodOverlay;
			return null;
		}
	}

	static class FoodOverlay
	{
		private FoodValues defaultFood;
		private FoodValues modifiedFood;

		private int biggestHunger;
		private float biggestSaturationIncrement;

		private int hungerBars;
		private String hungerBarsText;

		private int saturationBars;
		private String saturationBarsText;

		private String tooltip;

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

		String getTooltip()
		{
			if (tooltip != null)
			{
				return tooltip;
			}
			// 9x9 icon convert to scale of blank string.
			float scale = 2.2f;

			float hungerBarsLength = (float) hungerBars * scale;
			if (hungerBarsText != null)
			{
				hungerBarsLength += hungerBarsText.length();
			}

			float saturationBarsLength = (float) saturationBars * scale;
			if (saturationBarsText != null)
			{
				saturationBarsLength += saturationBarsText.length();
			}

			int length = (int) Math.ceil(Math.max(hungerBarsLength, saturationBarsLength * 0.8f));
			StringBuilder s = new StringBuilder(" ");
			for (int i = 0; i < length; i++)
			{
				s.append(" ");
			}

			tooltip = s.toString();
			return tooltip;
		}

		boolean shouldRenderHungerBars()
		{
			return hungerBars > 0;
		}
	}

	@SubscribeEvent
	public void onItemTooltip(ItemTooltipEvent event)
	{
		if (event.isCanceled())
			return;

		ItemStack hoveredStack = event.getItemStack();
		if (!shouldShowTooltip(hoveredStack))
			return;

		List<ITextComponent> tooltip = event.getToolTip();
		if (tooltip == null)
			return;

		Minecraft mc = Minecraft.getInstance();
		FoodValues defaultFood = FoodHelper.getDefaultFoodValues(hoveredStack);
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

		FoodOverlay foodOverlay = new FoodOverlay(prerenderEvent.itemStack, defaultFood, modifiedFood, mc.player);
		if (foodOverlay.shouldRenderHungerBars())
		{
			Style style = Style.EMPTY.setFontId(new FoodOverlayFont(foodOverlay));
			StringTextComponent placeholder = new StringTextComponent(foodOverlay.getTooltip());
			tooltip.add(placeholder.setStyle(style));
			tooltip.add(placeholder.setStyle(style));
		}
	}

	@SubscribeEvent
	public void onRenderTooltip(RenderTooltipEvent.PostText event)
	{
		if (event.isCanceled())
			return;

		Minecraft mc = Minecraft.getInstance();
		Screen gui = mc.currentScreen;
		if (gui == null)
			return;

		int toolTipY = event.getY();
		int toolTipX = event.getX();
		int toolTipZ = 400; // tooltip text zLevel is 400, hardcode in GuiUtils.

		// Find food overlay of text lines.
		FoodOverlay foodOverlay = null;
		List<? extends ITextProperties> lines = event.getLines();
		for (int i = 0; i < lines.size(); ++i)
		{
			foodOverlay = FoodOverlayFont.getFoodOverlay(lines.get(i));
			if (foodOverlay != null)
			{
				toolTipY += i * 10;
				break;
			}
		}

		// Not found overlay text lines, maybe some mods removed it.
		if (foodOverlay == null)
			return;

		MatrixStack matrixStack = event.getMatrixStack();
		ItemStack itemStack = foodOverlay.itemStack;
		FoodValues defaultFood = foodOverlay.defaultFood;
		FoodValues modifiedFood = foodOverlay.modifiedFood;

		// Notify everyone that we should render tooltip overlay
		TooltipOverlayEvent.Render renderEvent = new TooltipOverlayEvent.Render(itemStack, toolTipX, toolTipY, matrixStack, defaultFood, modifiedFood);
		MinecraftForge.EVENT_BUS.post(renderEvent);
		if (renderEvent.isCanceled())
			return;

		toolTipX = renderEvent.x;
		toolTipY = renderEvent.y;
		matrixStack = renderEvent.matrixStack;

		RenderSystem.disableLighting();
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();

		matrixStack.push();
		matrixStack.translate(0.0D, 0.0D, toolTipZ);

		int x = toolTipX;
		int y = toolTipY + 2;

		int defaultHunger = defaultFood.hunger;
		int modifiedHunger = modifiedFood.hunger;

		// Render from right to left so that the icons 'face' the right way
		x += (foodOverlay.hungerBars - 1) * 9;

		mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
		TextureOffsets offsets = FoodHelper.isRotten(itemStack) ? rottenBarTextureOffsets : normalBarTextureOffsets;
		for (int i = 0; i < foodOverlay.hungerBars * 2; i += 2)
		{

			if (modifiedHunger < 0)
				gui.blit(matrixStack, x, y, offsets.containerNegativeHunger, 27, 9, 9);
			else if (modifiedHunger > defaultHunger && defaultHunger <= i)
				gui.blit(matrixStack, x, y, offsets.containerExtraHunger, 27, 9, 9);
			else if (modifiedHunger > i + 1 || defaultHunger == modifiedHunger)
				gui.blit(matrixStack, x, y, offsets.containerNormalHunger, 27, 9, 9);
			else if (modifiedHunger == i + 1)
				gui.blit(matrixStack, x, y, offsets.containerPartialHunger, 27, 9, 9);
			else
			{
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, .5F);
				gui.blit(matrixStack, x, y, offsets.containerMissingHunger, 27, 9, 9);
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
			}

			RenderSystem.color4f(1.0F, 1.0F, 1.0F, .25F);
			gui.blit(matrixStack, x, y, defaultHunger - 1 == i ? offsets.shankMissingPartial : offsets.shankMissingFull, 27, 9, 9);
			RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

			if (modifiedHunger > i)
				gui.blit(matrixStack, x, y, modifiedHunger - 1 == i ? offsets.shankPartial : offsets.shankFull, 27, 9, 9);

			x -= 9;
		}
		if (foodOverlay.hungerBarsText != null)
		{
			x += 18;
			matrixStack.push();
			matrixStack.translate(x, y, 0);
			matrixStack.scale(0.75f, 0.75f, 0.75f);
			mc.fontRenderer.func_238406_a_(matrixStack, foodOverlay.hungerBarsText, 2, 2, 0xFFAAAAAA, false);
			matrixStack.pop();
		}

		x = toolTipX;
		y += 10;

		float modifiedSaturationIncrement = modifiedFood.getSaturationIncrement();
		float absModifiedSaturationIncrement = Math.abs(modifiedSaturationIncrement);

		// Render from right to left so that the icons 'face' the right way
		x += (foodOverlay.saturationBars - 1) * 7;

		RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(modIcons);
		for (int i = 0; i < foodOverlay.saturationBars * 2; i += 2)
		{
			float effectiveSaturationOfBar = (absModifiedSaturationIncrement - i) / 2f;

			boolean shouldBeFaded = absModifiedSaturationIncrement <= i;
			if (shouldBeFaded)
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, .5F);

			gui.blit(matrixStack, x, y, effectiveSaturationOfBar >= 1 ? 21 : effectiveSaturationOfBar > 0.5 ? 14 : effectiveSaturationOfBar > 0.25 ? 7 : effectiveSaturationOfBar > 0 ? 0 : 28, modifiedSaturationIncrement >= 0 ? 27 : 34, 7, 7);

			if (shouldBeFaded)
				RenderSystem.color4f(1.0F, 1.0F, 1.0F, 1.0F);

			x -= 7;
		}
		if (foodOverlay.saturationBarsText != null)
		{
			x += 14;
			matrixStack.push();
			matrixStack.translate(x, y, 0);
			matrixStack.scale(0.75f, 0.75f, 0.75f);
			mc.fontRenderer.func_238406_a_(matrixStack, foodOverlay.saturationBarsText, 2, 1, 0xFFAAAAAA, false);
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

	private boolean shouldShowTooltip(ItemStack hoveredStack)
	{
		if (hoveredStack.isEmpty())
			return false;

		boolean shouldShowTooltip = (ModConfig.SHOW_FOOD_VALUES_IN_TOOLTIP.get() && KeyHelper.isShiftKeyDown()) || ModConfig.ALWAYS_SHOW_FOOD_VALUES_TOOLTIP.get();
		if (!shouldShowTooltip)
			return false;

		if (!FoodHelper.isFood(hoveredStack))
			return false;

		return true;
	}
}
