package squeek.appleskin.gui;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.DummyConfigSerializer;
import net.minecraft.world.InteractionResult;
import squeek.appleskin.ModConfig;

import java.io.IOException;

@Config(name = "appleskin")
public class AutoConfigIntegration implements ConfigData
{
	public static void init()
	{
		var holder = AutoConfig.register(AutoConfigIntegration.class, DummyConfigSerializer::new);
		holder.registerSaveListener((manager, data) -> {
			ModConfig.INSTANCE.maxHudOverlayFlashAlpha = data.maxHudOverlayFlashAlpha;
			ModConfig.INSTANCE.showFoodHealthHudOverlay = data.showFoodHealthHudOverlay;
			ModConfig.INSTANCE.showFoodExhaustionHudUnderlay = data.showFoodExhaustionHudUnderlay;
			ModConfig.INSTANCE.showFoodValuesInTooltip = data.showFoodValuesInTooltip;
			ModConfig.INSTANCE.showFoodValuesHudOverlay = data.showFoodValuesHudOverlay;
			ModConfig.INSTANCE.showFoodValuesInTooltipAlways = data.showFoodValuesInTooltipAlways;
			ModConfig.INSTANCE.showSaturationHudOverlay = data.showSaturationHudOverlay;
			ModConfig.INSTANCE.showVanillaAnimationsOverlay = data.showVanillaAnimationsOverlay;
			ModConfig.INSTANCE.showFoodValuesHudOverlayWhenOffhand = data.showFoodValuesHudOverlayWhenOffhand;
			try
			{
				ModConfig.INSTANCE.save();
				return InteractionResult.SUCCESS;
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		});
		holder.registerLoadListener((manager, data) -> {
			data.maxHudOverlayFlashAlpha = ModConfig.INSTANCE.maxHudOverlayFlashAlpha;
			data.showFoodHealthHudOverlay = ModConfig.INSTANCE.showFoodHealthHudOverlay;
			data.showFoodExhaustionHudUnderlay = ModConfig.INSTANCE.showFoodExhaustionHudUnderlay;
			data.showFoodValuesInTooltip = ModConfig.INSTANCE.showFoodValuesInTooltip;
			data.showFoodValuesHudOverlay = ModConfig.INSTANCE.showFoodValuesHudOverlay;
			data.showFoodValuesInTooltipAlways = ModConfig.INSTANCE.showFoodValuesInTooltipAlways;
			data.showSaturationHudOverlay = ModConfig.INSTANCE.showSaturationHudOverlay;
			data.showVanillaAnimationsOverlay = ModConfig.INSTANCE.showVanillaAnimationsOverlay;
			data.showFoodValuesHudOverlayWhenOffhand = ModConfig.INSTANCE.showFoodValuesHudOverlayWhenOffhand;
			return InteractionResult.SUCCESS;
		});
		holder.load();
	}

	@ConfigEntry.Gui.Excluded
	private static final ModConfig DEFAULTS = new ModConfig();

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesInTooltip = DEFAULTS.showFoodValuesInTooltip;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesInTooltipAlways = DEFAULTS.showFoodValuesInTooltipAlways;

	@ConfigEntry.Gui.Tooltip()
	public boolean showSaturationHudOverlay = DEFAULTS.showSaturationHudOverlay;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesHudOverlay = DEFAULTS.showFoodValuesHudOverlay;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesHudOverlayWhenOffhand = DEFAULTS.showFoodValuesHudOverlayWhenOffhand;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodExhaustionHudUnderlay = DEFAULTS.showFoodExhaustionHudUnderlay;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodHealthHudOverlay = DEFAULTS.showFoodHealthHudOverlay;

	@ConfigEntry.Gui.Tooltip()
	public boolean showVanillaAnimationsOverlay = DEFAULTS.showVanillaAnimationsOverlay;

	@ConfigEntry.Gui.Tooltip()
	public float maxHudOverlayFlashAlpha = DEFAULTS.maxHudOverlayFlashAlpha;
}
