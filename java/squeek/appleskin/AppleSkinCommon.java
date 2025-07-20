package squeek.appleskin;

import net.fabricmc.api.ModInitializer;
import squeek.appleskin.network.SyncHandler;

public final class AppleSkinCommon implements ModInitializer
{
	@Override
	public void onInitialize()
	{
		SyncHandler.init();
	}
}
