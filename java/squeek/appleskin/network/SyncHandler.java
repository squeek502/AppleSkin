package squeek.appleskin.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.NetworkRegistry;
import net.neoforged.neoforge.network.PlayNetworkDirection;
import net.neoforged.neoforge.network.simple.SimpleChannel;
import squeek.appleskin.ModInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SyncHandler
{
	private static final String PROTOCOL_VERSION = Integer.toString(1);
	public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
			.named(new ResourceLocation(ModInfo.MODID, "sync"))
			.clientAcceptedVersions(s -> true)
			.serverAcceptedVersions(s -> true)
			.networkProtocolVersion(() -> PROTOCOL_VERSION)
			.simpleChannel();

	public static void init()
	{
		CHANNEL.registerMessage(1, MessageExhaustionSync.class, MessageExhaustionSync::encode, MessageExhaustionSync::decode, MessageExhaustionSync::handle);
		CHANNEL.registerMessage(2, MessageSaturationSync.class, MessageSaturationSync::encode, MessageSaturationSync::decode, MessageSaturationSync::handle);

		NeoForge.EVENT_BUS.register(new SyncHandler());
	}

	/*
	 * Sync saturation (vanilla MC only syncs when it hits 0)
	 * Sync exhaustion (vanilla MC does not sync it at all)
	 */
	private static final Map<UUID, Float> lastSaturationLevels = new HashMap<>();
	private static final Map<UUID, Float> lastExhaustionLevels = new HashMap<>();

	@SubscribeEvent
	public void onLivingTickEvent(LivingTickEvent event)
	{
		if (!(event.getEntity() instanceof ServerPlayer))
			return;

		ServerPlayer player = (ServerPlayer) event.getEntity();
		Float lastSaturationLevel = lastSaturationLevels.get(player.getUUID());
		Float lastExhaustionLevel = lastExhaustionLevels.get(player.getUUID());

		if (lastSaturationLevel == null || lastSaturationLevel != player.getFoodData().getSaturationLevel())
		{
			var msg = new MessageSaturationSync(player.getFoodData().getSaturationLevel());
			CHANNEL.sendTo(msg, player.connection.connection, PlayNetworkDirection.PLAY_TO_CLIENT);
			lastSaturationLevels.put(player.getUUID(), player.getFoodData().getSaturationLevel());
		}

		float exhaustionLevel = player.getFoodData().getExhaustionLevel();
		if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f)
		{
			var msg = new MessageExhaustionSync(exhaustionLevel);
			CHANNEL.sendTo(msg, player.connection.connection, PlayNetworkDirection.PLAY_TO_CLIENT);
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
	}
}
