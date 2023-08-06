package squeek.appleskin;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;


@Config(name = "appleskin")
public class ModConfig implements ConfigData
{

	@ConfigEntry.Gui.Excluded
	public static ModConfig INSTANCE;

	public static void init()
	{
		AutoConfig.register(ModConfig.class, JanksonConfigSerializer::new);
		INSTANCE = AutoConfig.getConfigHolder(ModConfig.class).getConfig();
	}

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesInTooltip = true;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesInTooltipAlways = true;

	@ConfigEntry.Gui.Tooltip()
	public boolean showSaturationHudOverlay = true;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesHudOverlay = true;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodValuesHudOverlayWhenOffhand = true;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodExhaustionHudUnderlay = true;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodHealthHudOverlay = true;

	@ConfigEntry.Gui.Tooltip()
	public boolean showFoodDebugInfo = true;

	@ConfigEntry.Gui.Tooltip()
	public boolean showVanillaAnimationsOverlay = true;

	@ConfigEntry.Gui.Tooltip()
	public float maxHudOverlayFlashAlpha = 0.65f;
}


