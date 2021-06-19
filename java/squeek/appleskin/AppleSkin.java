package squeek.appleskin;

import net.fabricmc.api.ClientModInitializer;
import squeek.appleskin.network.ClientSyncHandler;

public class AppleSkin implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		ClientSyncHandler.init();
		ModConfig.init();
	}
}
