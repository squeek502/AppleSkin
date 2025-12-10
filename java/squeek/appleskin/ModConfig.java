package squeek.appleskin;

import blue.endless.jankson.Comment;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.api.SyntaxError;
import net.fabricmc.loader.api.FabricLoader;
import squeek.appleskin.gui.AutoConfigIntegration;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


public class ModConfig
{
	public static ModConfig INSTANCE;
	public static Jankson JANKSON;
	public static Path PATH;

	public static void init()
	{
		PATH = FabricLoader.getInstance().getConfigDir().resolve("appleskin.json5");
		JANKSON = Jankson.builder().build();
		INSTANCE = new ModConfig();
		try
		{
			INSTANCE.load();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
		if (FabricLoader.getInstance().isModLoaded("cloth-config"))
		{
			AutoConfigIntegration.init();
		}
	}

	public void save() throws IOException
	{
		Files.createDirectories(PATH.getParent());
		BufferedWriter writer = Files.newBufferedWriter(PATH);
		writer.write(JANKSON.toJson(INSTANCE).toJson(true, true));
		writer.close();
	}

	public void load() throws IOException, SyntaxError
	{
		if (Files.exists(PATH))
		{
			var object = JANKSON.load(PATH.toFile());
			INSTANCE = JANKSON.fromJson(object, ModConfig.class);
		}
		else
		{
			save();
		}
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


