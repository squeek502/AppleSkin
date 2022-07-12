package squeek.appleskin;

import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import squeek.appleskin.client.DebugInfoHandler;
import squeek.appleskin.client.HUDOverlayHandler;
import squeek.appleskin.client.TooltipOverlayHandler;
import squeek.appleskin.network.SyncHandler;

@Mod(ModInfo.MODID)
public class AppleSkin
{
	public static Logger Log = LogManager.getLogger(ModInfo.MODID);

	public AppleSkin()
	{
		FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInit);
		if (FMLEnvironment.dist.isClient())
		{
			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::preInitClient);
			FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onRegisterClientTooltipComponentFactories);
		}
		ModLoadingContext.get().registerConfig(
			net.minecraftforge.fml.config.ModConfig.Type.CLIENT,
			ModConfig.SPEC
		);
		ModConfig.init(FMLPaths.CONFIGDIR.get().resolve(ModInfo.MODID + "-client.toml"));

		// Register ourselves for server and other game events we are interested in
		MinecraftForge.EVENT_BUS.register(this);
	}

	private void preInit(final FMLCommonSetupEvent event)
	{
		SyncHandler.init();
	}

	private void preInitClient(final FMLClientSetupEvent event)
	{
		DebugInfoHandler.init();
		HUDOverlayHandler.init();
		TooltipOverlayHandler.init();
	}

	private void onRegisterClientTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event)
	{
		TooltipOverlayHandler.register(event);
	}
}
