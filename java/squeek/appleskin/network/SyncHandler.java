package squeek.appleskin.network;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import squeek.appleskin.helpers.ExhaustionHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SyncHandler
{
	/*
	 * Sync saturation (vanilla MC only syncs when it hits 0)
	 * Sync exhaustion (vanilla MC does not sync it at all)
	 */
	private static final Map<UUID, Float> LAST_SATURATION_LEVELS = new HashMap<>();

	public static void init()
	{
		PayloadTypeRegistry.playS2C().register(ExhaustionSyncPayload.ID, ExhaustionSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(SaturationSyncPayload.ID, SaturationSyncPayload.CODEC);
		PayloadTypeRegistry.playS2C().register(NaturalRegenerationSyncPayload.ID, NaturalRegenerationSyncPayload.CODEC);
		ServerTickEvents.END_WORLD_TICK.register(SyncHandler::onServerWorldTick);
	}

	private SyncHandler() {
		throw new UnsupportedOperationException();
	}
	private static final Map<UUID, Float> LAST_EXHAUSTION_LEVELS = new HashMap<>();
	private static boolean naturalRegeneration = true;

	public static void onPlayerUpdate(ServerPlayerEntity player)
	{
		Float lastSaturationLevel = LAST_SATURATION_LEVELS.get(player.getUuid());
		Float lastExhaustionLevel = LAST_EXHAUSTION_LEVELS.get(player.getUuid());

		float saturation = player.getHungerManager().getSaturationLevel();
		if (lastSaturationLevel == null || lastSaturationLevel != saturation)
		{
			ServerPlayNetworking.send(player, new SaturationSyncPayload(saturation));
			LAST_SATURATION_LEVELS.put(player.getUuid(), saturation);
		}

		float exhaustionLevel = ExhaustionHelper.getExhaustion(player);
		if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f)
		{
			ServerPlayNetworking.send(player, new ExhaustionSyncPayload(exhaustionLevel));
			LAST_EXHAUSTION_LEVELS.put(player.getUuid(), exhaustionLevel);
		}
	}

	public static void onPlayerLoggedIn(ServerPlayerEntity player)
	{
		LAST_SATURATION_LEVELS.remove(player.getUuid());
		LAST_EXHAUSTION_LEVELS.remove(player.getUuid());
		// Assumed to be true by default, so we only need to update the client if it's actually false
		if (!naturalRegeneration) {
			ServerPlayNetworking.send(player, new NaturalRegenerationSyncPayload(false));
		}
	}

	public static void onServerWorldTick(ServerWorld world)
	{
		var cur = world.getGameRules().getBoolean(GameRules.NATURAL_REGENERATION);
		if (naturalRegeneration != cur) {
			for (ServerPlayerEntity player : world.getPlayers()) {
				ServerPlayNetworking.send(player, new NaturalRegenerationSyncPayload(cur));
			}
			naturalRegeneration = cur;
		}
	}
}
