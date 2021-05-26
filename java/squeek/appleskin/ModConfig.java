package squeek.appleskin;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.cloth.clothconfig.shadowed.blue.endless.jankson.Comment;


@Config(name = "appleskin")
public class ModConfig implements ConfigData {

    @ConfigEntry.Gui.Excluded
    public static ModConfig INSTANCE;

    public static void init() {
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

    @Comment("If true, shows the hunger (and saturation if showSaturationHudOverlay is true) that would be restored by food you are offhand holding")
    public boolean showFoodValuesHudOverlayWhenOffhand = true;

    @Comment("If true, shows your food exhaustion as a progress bar behind the hunger bars")
    public boolean showFoodExhaustionHudUnderlay = true;
}


