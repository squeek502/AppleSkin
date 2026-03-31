package squeek.appleskin.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import squeek.appleskin.helpers.ExhaustionHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SyncHandler
{
	public static void init()
	{
		PayloadTypeRegistry.clientboundPlay().register(ExhaustionSyncPayload.ID, ExhaustionSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(SaturationSyncPayload.ID, SaturationSyncPayload.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(NaturalRegenerationSyncPayload.ID, NaturalRegenerationSyncPayload.CODEC);
		ServerTickEvents.END_LEVEL_TICK.register(SyncHandler::onServerWorldTick);
	}

	/*
	 * Sync saturation (vanilla MC only syncs when it hits 0)
	 * Sync exhaustion (vanilla MC does not sync it at all)
	 */
	private static final Map<UUID, Float> lastSaturationLevels = new HashMap<UUID, Float>();
	private static final Map<UUID, Float> lastExhaustionLevels = new HashMap<UUID, Float>();
	private static boolean naturalRegeneration = true;

	public static void onPlayerUpdate(ServerPlayer player)
	{
		Float lastSaturationLevel = lastSaturationLevels.get(player.getUUID());
		Float lastExhaustionLevel = lastExhaustionLevels.get(player.getUUID());

		float saturation = player.getFoodData().getSaturationLevel();
		if (lastSaturationLevel == null || lastSaturationLevel != saturation)
		{
			ServerPlayNetworking.send(player, new SaturationSyncPayload(saturation));
			lastSaturationLevels.put(player.getUUID(), saturation);
		}

		float exhaustionLevel = ExhaustionHelper.getExhaustion(player);
		if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f)
		{
			ServerPlayNetworking.send(player, new ExhaustionSyncPayload(exhaustionLevel));
			lastExhaustionLevels.put(player.getUUID(), exhaustionLevel);
		}
	}

	public static void onPlayerLoggedIn(ServerPlayer player)
	{
		lastSaturationLevels.remove(player.getUUID());
		lastExhaustionLevels.remove(player.getUUID());
		// Assumed to be true by default, so we only need to update the client if it's actually false
		if (!naturalRegeneration) {
			ServerPlayNetworking.send(player, new NaturalRegenerationSyncPayload(false));
		}
	}

	public static void onServerWorldTick(ServerLevel world)
	{
		var cur = world.getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
		if (naturalRegeneration != cur) {
			for (ServerPlayer player : world.players()) {
				ServerPlayNetworking.send(player, new NaturalRegenerationSyncPayload(cur));
			}
			naturalRegeneration = cur;
		}
	}
}
