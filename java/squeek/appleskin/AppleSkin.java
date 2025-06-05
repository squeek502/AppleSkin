package squeek.appleskin;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
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

	public AppleSkin(IEventBus modEventBus, ModContainer container)
	{
		modEventBus.addListener(this::onRegisterPayloadHandler);
		if (FMLEnvironment.dist.isClient())
		{
			modEventBus.addListener(this::preInitClient);
			modEventBus.addListener(this::onRegisterHudHandler);
			modEventBus.addListener(this::onRegisterClientTooltipComponentFactories);
		}
		container.registerConfig(
			net.neoforged.fml.config.ModConfig.Type.CLIENT,
			ModConfig.SPEC
		);
		if (FMLEnvironment.dist.isClient())
		{
			container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
		}
	}

	private void preInitClient(final FMLClientSetupEvent event)
	{
		DebugInfoHandler.init();
		TooltipOverlayHandler.init();
	}

	private void onRegisterClientTooltipComponentFactories(RegisterClientTooltipComponentFactoriesEvent event)
	{
		TooltipOverlayHandler.register(event);
	}

	@SubscribeEvent
	private void onRegisterPayloadHandler(final RegisterPayloadHandlersEvent event)
	{
		SyncHandler.register(event);
	}

	@SubscribeEvent
	private void onRegisterHudHandler(final RegisterGuiLayersEvent event)
	{
		HUDOverlayHandler.register(event);
	}
}
