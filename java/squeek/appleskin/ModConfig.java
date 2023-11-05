package squeek.appleskin;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.nio.file.Path;

public class ModConfig
{
	public static void init(Path file)
	{
		final CommentedFileConfig configData = CommentedFileConfig.builder(file)
			.sync()
			.autosave()
			.writingMode(WritingMode.REPLACE)
			.build();

		configData.load();
		SPEC.setConfig(configData);
	}

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

	/*
	 * CLIENT
	 */
	public static final String CATEGORY_CLIENT = "client";
	private static final String CATEGORY_CLIENT_COMMENT =
		"These config settings are client-side only";

	public static final ModConfigSpec.BooleanValue SHOW_FOOD_VALUES_IN_TOOLTIP;
	public static boolean SHOW_FOOD_VALUES_IN_TOOLTIP_DEFAULT = true;
	private static final String SHOW_FOOD_VALUES_IN_TOOLTIP_NAME = "showFoodValuesInTooltip";
	private static final String SHOW_FOOD_VALUES_IN_TOOLTIP_COMMENT =
		"If true, shows the hunger and saturation values of food in its tooltip while holding SHIFT";

	public static final ModConfigSpec.BooleanValue ALWAYS_SHOW_FOOD_VALUES_TOOLTIP;
	public static boolean ALWAYS_SHOW_FOOD_VALUES_TOOLTIP_DEFAULT = true;
	private static final String ALWAYS_SHOW_FOOD_VALUES_TOOLTIP_NAME = "showFoodValuesInTooltipAlways";
	private static final String ALWAYS_SHOW_FOOD_VALUES_TOOLTIP_COMMENT =
		"If true, shows the hunger and saturation values of food in its tooltip automatically (without needing to hold SHIFT)";

	public static final ModConfigSpec.BooleanValue SHOW_SATURATION_OVERLAY;
	public static boolean SHOW_SATURATION_OVERLAY_DEFAULT = true;
	private static final String SHOW_SATURATION_OVERLAY_NAME = "showSaturationHudOverlay";
	private static final String SHOW_SATURATION_OVERLAY_COMMENT =
		"If true, shows your current saturation level overlayed on the hunger bar";

	public static final ModConfigSpec.BooleanValue SHOW_FOOD_VALUES_OVERLAY;
	public static boolean SHOW_FOOD_VALUES_OVERLAY_DEFAULT = true;
	private static final String SHOW_FOOD_VALUES_OVERLAY_NAME = "showFoodValuesHudOverlay";
	private static final String SHOW_FOOD_VALUES_OVERLAY_COMMENT =
		"If true, shows the hunger (and saturation if " + SHOW_SATURATION_OVERLAY_NAME + " is true) that would be restored by food you are currently holding";

	public static final ModConfigSpec.BooleanValue SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND;
	public static boolean SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND_DEFAULT = true;
	private static final String SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND_NAME = "showFoodValuesHudOverlayWhenOffhand";
	private static final String SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND_COMMENT =
		"If true, enables the hunger/saturation/health overlays for food in your off-hand";

	public static final ModConfigSpec.BooleanValue SHOW_FOOD_EXHAUSTION_UNDERLAY;
	public static boolean SHOW_FOOD_EXHAUSTION_UNDERLAY_DEFAULT = true;
	private static final String SHOW_FOOD_EXHAUSTION_UNDERLAY_NAME = "showFoodExhaustionHudUnderlay";
	private static final String SHOW_FOOD_EXHAUSTION_UNDERLAY_COMMENT =
		"If true, shows your food exhaustion as a progress bar behind the hunger bars";

	public static final ModConfigSpec.BooleanValue SHOW_FOOD_DEBUG_INFO;
	public static boolean SHOW_FOOD_DEBUG_INFO_DEFAULT = true;
	private static final String SHOW_FOOD_DEBUG_INFO_NAME = "showFoodStatsInDebugOverlay";
	private static final String SHOW_FOOD_DEBUG_INFO_COMMENT =
		"If true, adds a line that shows your hunger, saturation, and exhaustion level in the F3 debug overlay";

	public static final ModConfigSpec.BooleanValue SHOW_FOOD_HEALTH_HUD_OVERLAY;
	public static boolean SHOW_FOOD_HEALTH_HUD_OVERLAY_DEFAULT = true;
	private static final String SHOW_FOOD_HEALTH_HUD_OVERLAY_NAME = "showFoodHealthHudOverlay";
	private static final String SHOW_FOOD_HEALTH_HUD_OVERLAY_COMMENT =
		"If true, shows estimated health restored by food on the health bar";

	public static final ModConfigSpec.BooleanValue SHOW_VANILLA_ANIMATION_OVERLAY;
	public static boolean SHOW_VANILLA_ANIMATION_OVERLAY_DEFAULT = true;
	private static final String SHOW_VANILLA_ANIMATION_OVERLAY_NAME = "showVanillaAnimationsOverlay";
	private static final String SHOW_VANILLA_ANIMATION_OVERLAY_COMMENT =
		"If true, health/hunger overlay will shake to match Minecraft's icon animations";

	public static final ModConfigSpec.DoubleValue MAX_HUD_OVERLAY_FLASH_ALPHA;
	public static double MAX_HUD_OVERLAY_FLASH_ALPHA_DEFAULT = 0.65D;
	private static final String MAX_HUD_OVERLAY_FLASH_ALPHA_NAME = "maxHudOverlayFlashAlpha";
	private static final String MAX_HUD_OVERLAY_FLASH_ALPHA_COMMENT =
		"Alpha value of the flashing icons at their most visible point (1.0 = fully opaque, 0.0 = fully transparent)";

	static
	{
		BUILDER.push(CATEGORY_CLIENT);
		SHOW_FOOD_VALUES_IN_TOOLTIP = BUILDER
			.comment(SHOW_FOOD_VALUES_IN_TOOLTIP_COMMENT)
			.define(SHOW_FOOD_VALUES_IN_TOOLTIP_NAME, SHOW_FOOD_VALUES_IN_TOOLTIP_DEFAULT);
		ALWAYS_SHOW_FOOD_VALUES_TOOLTIP = BUILDER
			.comment(ALWAYS_SHOW_FOOD_VALUES_TOOLTIP_COMMENT)
			.define(ALWAYS_SHOW_FOOD_VALUES_TOOLTIP_NAME, ALWAYS_SHOW_FOOD_VALUES_TOOLTIP_DEFAULT);
		SHOW_SATURATION_OVERLAY = BUILDER
			.comment(SHOW_SATURATION_OVERLAY_COMMENT)
			.define(SHOW_SATURATION_OVERLAY_NAME, SHOW_SATURATION_OVERLAY_DEFAULT);
		SHOW_FOOD_VALUES_OVERLAY = BUILDER
			.comment(SHOW_FOOD_VALUES_OVERLAY_COMMENT)
			.define(SHOW_FOOD_VALUES_OVERLAY_NAME, SHOW_FOOD_VALUES_OVERLAY_DEFAULT);
		SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND = BUILDER
			.comment(SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND_COMMENT)
			.define(SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND_NAME, SHOW_FOOD_VALUES_OVERLAY_WHEN_OFFHAND_DEFAULT);
		SHOW_FOOD_EXHAUSTION_UNDERLAY = BUILDER
			.comment(SHOW_FOOD_EXHAUSTION_UNDERLAY_COMMENT)
			.define(SHOW_FOOD_EXHAUSTION_UNDERLAY_NAME, SHOW_FOOD_EXHAUSTION_UNDERLAY_DEFAULT);
		SHOW_FOOD_DEBUG_INFO = BUILDER
			.comment(SHOW_FOOD_DEBUG_INFO_COMMENT)
			.define(SHOW_FOOD_DEBUG_INFO_NAME, SHOW_FOOD_DEBUG_INFO_DEFAULT);
		SHOW_FOOD_HEALTH_HUD_OVERLAY = BUILDER
			.comment(SHOW_FOOD_HEALTH_HUD_OVERLAY_COMMENT)
			.define(SHOW_FOOD_HEALTH_HUD_OVERLAY_NAME, SHOW_FOOD_HEALTH_HUD_OVERLAY_DEFAULT);
		SHOW_VANILLA_ANIMATION_OVERLAY = BUILDER
			.comment(SHOW_VANILLA_ANIMATION_OVERLAY_COMMENT)
			.define(SHOW_VANILLA_ANIMATION_OVERLAY_NAME, SHOW_VANILLA_ANIMATION_OVERLAY_DEFAULT);
		MAX_HUD_OVERLAY_FLASH_ALPHA = BUILDER
			.comment(MAX_HUD_OVERLAY_FLASH_ALPHA_COMMENT)
			.defineInRange(MAX_HUD_OVERLAY_FLASH_ALPHA_NAME, MAX_HUD_OVERLAY_FLASH_ALPHA_DEFAULT, 0D, 1D);
		BUILDER.pop();
	}

	public static final ModConfigSpec SPEC = BUILDER.build();
}