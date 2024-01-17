package squeek.appleskin.network;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEvent.LivingTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import squeek.appleskin.ModInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SyncHandler
{
	private static final String PROTOCOL_VERSION = "1.0.0";

	public static void register(final RegisterPayloadHandlerEvent event)
	{
		final IPayloadRegistrar registrar = event.registrar(ModInfo.MODID)
			.versioned(PROTOCOL_VERSION)
			.optional();
		registrar.play(MessageExhaustionSync.ID,
			MessageExhaustionSync::new,
			handler -> handler.client(MessageExhaustionSync::handle));
		registrar.play(MessageSaturationSync.ID,
			MessageSaturationSync::new,
			handler -> handler.client(MessageSaturationSync::handle));

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
			PacketDistributor.PLAYER.with(player).send(msg);
			lastSaturationLevels.put(player.getUUID(), player.getFoodData().getSaturationLevel());
		}

		float exhaustionLevel = player.getFoodData().getExhaustionLevel();
		if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f)
		{
			var msg = new MessageExhaustionSync(exhaustionLevel);
			PacketDistributor.PLAYER.with(player).send(msg);
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
