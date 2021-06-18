package squeek.appleskin;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;


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

	@Comment("If true, shows the hunger and saturation values of food in its tooltip while holding SHIFT")
	public boolean showFoodValuesInTooltip = true;

	@Comment("If true, shows the hunger and saturation values of food in its tooltip automatically (without needing to hold SHIFT)")
	public boolean showFoodValuesInTooltipAlways = true;

	@Comment("If true, shows your current saturation level overlayed on the hunger bar")
	public boolean showSaturationHudOverlay = true;

	@Comment("If true, shows the hunger (and saturation if showSaturationHudOverlay is true) that would be restored by food you are currently holding")
	public boolean showFoodValuesHudOverlay = true;

	@Comment("If true, enables the hunger/saturation/health overlays for food in your off-hand")
	public boolean showFoodValuesHudOverlayWhenOffhand = true;

	@Comment("If true, shows your food exhaustion as a progress bar behind the hunger bar")
	public boolean showFoodExhaustionHudUnderlay = true;

	@Comment("If true, shows estimated health restored by food on the health bar")
	public boolean showFoodHealthHudOverlay = true;

	@Comment("If true, health/hunger overlay will shake to match Minecraft's icon animations")
	public boolean showVanillaAnimationsOverlay = true;

	@Comment("Alpha value of the flashing icons at their most visible point (1.0 = fully opaque, 0.0 = fully transparent)")
	public float maxHudOverlayFlashAlpha = 0.65f;
}


