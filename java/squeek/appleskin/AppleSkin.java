package squeek.appleskin;

import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.common.NeoForge;
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
			net.neoforged.fml.config.ModConfig.Type.CLIENT,
			ModConfig.SPEC
		);
		ModConfig.init(FMLPaths.CONFIGDIR.get().resolve(ModInfo.MODID + "-client.toml"));
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
