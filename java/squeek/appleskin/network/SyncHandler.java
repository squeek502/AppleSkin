package squeek.appleskin.network;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import squeek.appleskin.ModInfo;
import squeek.appleskin.helpers.HungerHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SyncHandler
{
	private static final String PROTOCOL_VERSION = Integer.toString(1);
	public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
		.named(new ResourceLocation(ModInfo.MODID, "sync"))
		.clientAcceptedVersions(PROTOCOL_VERSION::equals)
		.serverAcceptedVersions(PROTOCOL_VERSION::equals)
		.networkProtocolVersion(() -> PROTOCOL_VERSION)
		.simpleChannel();

	public static void init()
	{
		CHANNEL.registerMessage(1, MessageExhaustionSync.class, MessageExhaustionSync::encode, MessageExhaustionSync::decode, MessageExhaustionSync::handle);
		CHANNEL.registerMessage(2, MessageSaturationSync.class, MessageSaturationSync::encode, MessageSaturationSync::decode, MessageSaturationSync::handle);

		MinecraftForge.EVENT_BUS.register(new SyncHandler());
	}

	/*
	 * Sync saturation (vanilla MC only syncs when it hits 0)
	 * Sync exhaustion (vanilla MC does not sync it at all)
	 */
	private static final Map<UUID, Float> lastSaturationLevels = new HashMap<>();
	private static final Map<UUID, Float> lastExhaustionLevels = new HashMap<>();

	@SubscribeEvent
	public void onLivingUpdateEvent(LivingUpdateEvent event)
	{
		if (!(event.getEntity() instanceof EntityPlayerMP))
			return;

		EntityPlayerMP player = (EntityPlayerMP) event.getEntity();
		Float lastSaturationLevel = lastSaturationLevels.get(player.getUniqueID());
		Float lastExhaustionLevel = lastExhaustionLevels.get(player.getUniqueID());

		if (lastSaturationLevel == null || lastSaturationLevel != player.getFoodStats().getSaturationLevel())
		{
			Object msg = new MessageSaturationSync(player.getFoodStats().getSaturationLevel());
			CHANNEL.sendTo(msg, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
			lastSaturationLevels.put(player.getUniqueID(), player.getFoodStats().getSaturationLevel());
		}

		float exhaustionLevel = HungerHelper.getExhaustion(player);
		if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f)
		{
			Object msg = new MessageExhaustionSync(exhaustionLevel);
			CHANNEL.sendTo(msg, player.connection.netManager, NetworkDirection.PLAY_TO_CLIENT);
			lastExhaustionLevels.put(player.getUniqueID(), exhaustionLevel);
		}
	}

	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
	{
		if (!(event.getPlayer() instanceof EntityPlayerMP))
			return;

		lastSaturationLevels.remove(event.getPlayer().getUniqueID());
		lastExhaustionLevels.remove(event.getPlayer().getUniqueID());
	}
}
