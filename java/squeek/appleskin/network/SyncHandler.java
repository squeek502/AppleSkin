package squeek.appleskin.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import squeek.appleskin.ModInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SyncHandler
{
	private static final String PROTOCOL_VERSION = "1.0.0";

	public static void register(final RegisterPayloadHandlersEvent event)
	{
		final PayloadRegistrar registrar = event.registrar(ModInfo.MODID)
			.versioned(PROTOCOL_VERSION)
			.optional();

		registrar.playToClient(MessageExhaustionSync.TYPE, MessageExhaustionSync.CODEC, MessageExhaustionSync::handle);
		registrar.playToClient(MessageSaturationSync.TYPE, MessageSaturationSync.CODEC, MessageSaturationSync::handle);
		registrar.playToClient(MessageNaturalRegenerationSync.TYPE, MessageNaturalRegenerationSync.CODEC, MessageNaturalRegenerationSync::handle);

		NeoForge.EVENT_BUS.register(new SyncHandler());
	}

	/*
	 * Sync saturation (vanilla MC only syncs when it hits 0)
	 * Sync exhaustion (vanilla MC does not sync it at all)
	 */
	private static final Map<UUID, Float> lastSaturationLevels = new HashMap<>();
	private static final Map<UUID, Float> lastExhaustionLevels = new HashMap<>();
	private static boolean naturalRegeneration = true;

	@SubscribeEvent
	public void onLivingTickEvent(EntityTickEvent.Pre event)
	{
		if (!(event.getEntity() instanceof ServerPlayer))
			return;

		ServerPlayer player = (ServerPlayer) event.getEntity();
		Float lastSaturationLevel = lastSaturationLevels.get(player.getUUID());
		Float lastExhaustionLevel = lastExhaustionLevels.get(player.getUUID());

		if (lastSaturationLevel == null || lastSaturationLevel != player.getFoodData().getSaturationLevel())
		{
			var msg = new MessageSaturationSync(player.getFoodData().getSaturationLevel());
			PacketDistributor.sendToPlayer(player, msg);
			lastSaturationLevels.put(player.getUUID(), player.getFoodData().getSaturationLevel());
		}

		float exhaustionLevel = player.getFoodData().exhaustionLevel;
		if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f)
		{
			var msg = new MessageExhaustionSync(exhaustionLevel);
			PacketDistributor.sendToPlayer(player, msg);
			lastExhaustionLevels.put(player.getUUID(), exhaustionLevel);
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (!(event.getEntity() instanceof ServerPlayer))
			return;

		lastSaturationLevels.remove(event.getEntity().getUUID());
		lastExhaustionLevels.remove(event.getEntity().getUUID());
		// Assumed to be true by default, so we only need to update the client if it's actually false
		if (!naturalRegeneration) {
			PacketDistributor.sendToPlayer((ServerPlayer)event.getEntity(), new MessageNaturalRegenerationSync(false));
		}
	}

	@SubscribeEvent
	public void onServerWorldTick(ServerTickEvent.Post event)
	{
		var cur = event.getServer().getWorldData().getGameRules().get(GameRules.NATURAL_HEALTH_REGENERATION);
		if (naturalRegeneration != cur) {
			PacketDistributor.sendToAllPlayers(new MessageNaturalRegenerationSync(cur));
			naturalRegeneration = cur;
		}
	}
}
