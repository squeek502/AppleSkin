package squeek.appleskin.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.SimpleChannel;
import squeek.appleskin.ModInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SyncHandler
{
	private static final int PROTOCOL_VERSION = 1;
	public static final SimpleChannel CHANNEL = ChannelBuilder
		.named(new ResourceLocation(ModInfo.MODID, "sync"))
		.optional()
		.acceptedVersions((status, version) -> true)
		.networkProtocolVersion(PROTOCOL_VERSION)
		.simpleChannel();

	public static void init()
	{
		CHANNEL.messageBuilder(MessageExhaustionSync.class, NetworkDirection.PLAY_TO_CLIENT)
			.encoder(MessageExhaustionSync::encode)
			.decoder(MessageExhaustionSync::decode)
			.consumerNetworkThread(MessageExhaustionSync::handle)
			.add();
		CHANNEL.messageBuilder(MessageSaturationSync.class, NetworkDirection.PLAY_TO_CLIENT)
			.encoder(MessageSaturationSync::encode)
			.decoder(MessageSaturationSync::decode)
			.consumerNetworkThread(MessageSaturationSync::handle)
			.add();
		MinecraftForge.EVENT_BUS.register(new SyncHandler());
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
			CHANNEL.send(msg, PacketDistributor.PLAYER.with(player));
			lastSaturationLevels.put(player.getUUID(), player.getFoodData().getSaturationLevel());
		}

		float exhaustionLevel = player.getFoodData().getExhaustionLevel();
		if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f)
		{
			var msg = new MessageExhaustionSync(exhaustionLevel);
			CHANNEL.send(msg, PacketDistributor.PLAYER.with(player));
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
