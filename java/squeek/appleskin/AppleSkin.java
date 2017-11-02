package squeek.appleskin;

import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import squeek.appleskin.client.DebugInfoHandler;
import squeek.appleskin.client.HUDOverlayHandler;
import squeek.appleskin.client.TooltipOverlayHandler;
import squeek.appleskin.helpers.BetterWithModsHelper;
import squeek.appleskin.network.SyncHandler;

@Mod(modid = ModInfo.MODID, version = ModInfo.VERSION, acceptableRemoteVersions = "*", guiFactory = ModInfo.GUI_FACTORY_CLASS, dependencies = "after:jei@[3.8.1,); required-after:forge@[13.19.0,); after:applecore@[2.2.0,)")
public class AppleSkin
{
	public static Logger Log = LogManager.getLogger(ModInfo.MODID);
	public static boolean hasAppleCore = false;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		hasAppleCore = Loader.isModLoaded("AppleCore") || Loader.isModLoaded("applecore");
		ModConfig.init(event.getSuggestedConfigurationFile());
		BetterWithModsHelper.init();
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		SyncHandler.init();

		if (event.getSide() == Side.CLIENT)
		{
			DebugInfoHandler.init();
			HUDOverlayHandler.init();
			TooltipOverlayHandler.init();
		}
	}
}
