package squeek.appleskin.network;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import squeek.appleskin.helpers.*;

public class ClientSyncHandler
{
	public static boolean naturalRegeneration = true;
	@Environment(EnvType.CLIENT)
	public static void init()
	{
		ClientPlayNetworking.registerGlobalReceiver(ExhaustionSyncPayload.ID, (payload, context) -> context.client().execute(() -> {
			if (context.client().player != null) ExhaustionHelper.setSaturation(context.client().player, payload.getExhaustion());
		}));

		ClientPlayNetworking.registerGlobalReceiver(SaturationSyncPayload.ID, (payload, context) -> context.client().execute(() -> {
			if (context.client().player != null) context.client().player.getFoodData().setSaturation(payload.getSaturation());
		}));

		ClientPlayNetworking.registerGlobalReceiver(NaturalRegenerationSyncPayload.ID, (payload, _) ->
			naturalRegeneration = payload.naturalRegeneration());
	}
}
