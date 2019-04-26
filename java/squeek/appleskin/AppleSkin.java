package squeek.appleskin;

import net.fabricmc.api.ClientModInitializer;
import squeek.appleskin.network.SyncHandler;

public class AppleSkin implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		SyncHandler.init();
	}
}
