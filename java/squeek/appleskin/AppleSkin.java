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
import squeek.appleskin.network.SyncHandler;

@Mod(modid = ModInfo.MODID, version = ModInfo.VERSION, acceptableRemoteVersions = "*", guiFactory = ModInfo.GUI_FACTORY_CLASS, dependencies = "after:JEI@[3.8.1,); required-after:Forge@[12.18.1.2053,)")
public class AppleSkin
{
	public static Logger Log = LogManager.getLogger(ModInfo.MODID);
	public static boolean hasAppleCore = false;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		hasAppleCore = Loader.isModLoaded("AppleCore") || Loader.isModLoaded("applecore");
		ModConfig.init(event.getSuggestedConfigurationFile());
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
