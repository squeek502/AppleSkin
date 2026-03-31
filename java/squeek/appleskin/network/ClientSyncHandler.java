package squeek.appleskin.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import squeek.appleskin.helpers.ExhaustionHelper;

public class ClientSyncHandler
{
	public static boolean naturalRegeneration = true;
	@Environment(EnvType.CLIENT)
	public static void init()
	{
		ClientPlayNetworking.registerGlobalReceiver(ExhaustionSyncPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				ExhaustionHelper.setExhaustion(context.client().player, payload.getExhaustion());
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(SaturationSyncPayload.ID, (payload, context) -> {
			context.client().execute(() -> {
				context.client().player.getFoodData().setSaturation(payload.getSaturation());
			});
		});
		ClientPlayNetworking.registerGlobalReceiver(NaturalRegenerationSyncPayload.ID, (payload, context) -> {
			naturalRegeneration = payload.naturalRegeneration();
		});
	}
}
