package squeek.appleskin;

import com.google.common.eventbus.EventBus;
import net.fabricmc.api.ClientModInitializer;
import squeek.appleskin.network.SyncHandler;

public class AppleSkin implements ClientModInitializer
{
	/**
	 * The core AppleSkin EventBusses, all events for AppleSkin will be fired on these,
	 * you should use this to register all your listeners.
	 */
	public static final EventBus EVENT_BUS = new EventBus("squeek.appleskin.AppleSkin.EventBus");

	@Override
	public void onInitializeClient()
	{
		SyncHandler.init();
	}
}
