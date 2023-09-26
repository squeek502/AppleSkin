package squeek.appleskin.network;

import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SyncHandler
{
	public static final Identifier EXHAUSTION_SYNC = new Identifier("appleskin", "exhaustion_sync");
	public static final Identifier SATURATION_SYNC = new Identifier("appleskin", "saturation_sync");

	private static PacketByteBuf makePacketBuf(float val)
	{
		PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
		buf.writeFloat(val);
		return buf;
	}

	/*
	 * Sync saturation (vanilla MC only syncs when it hits 0)
	 * Sync exhaustion (vanilla MC does not sync it at all)
	 */
	private static final Map<UUID, Float> lastSaturationLevels = new HashMap<UUID, Float>();
	private static final Map<UUID, Float> lastExhaustionLevels = new HashMap<UUID, Float>();

	public static void onPlayerUpdate(ServerPlayerEntity player)
	{
		Float lastSaturationLevel = lastSaturationLevels.get(player.getUuid());
		Float lastExhaustionLevel = lastExhaustionLevels.get(player.getUuid());

		float saturation = player.getHungerManager().getSaturationLevel();
		if (lastSaturationLevel == null || lastSaturationLevel != saturation)
		{
			ServerPlayNetworking.send(player, SATURATION_SYNC, makePacketBuf(saturation));
			lastSaturationLevels.put(player.getUuid(), saturation);
		}

		float exhaustionLevel = player.getHungerManager().getExhaustion();
		if (lastExhaustionLevel == null || Math.abs(lastExhaustionLevel - exhaustionLevel) >= 0.01f)
		{
			ServerPlayNetworking.send(player, EXHAUSTION_SYNC, makePacketBuf(exhaustionLevel));
			lastExhaustionLevels.put(player.getUuid(), exhaustionLevel);
		}
	}

	public static void onPlayerLoggedIn(ServerPlayerEntity player)
	{
		lastSaturationLevels.remove(player.getUuid());
		lastExhaustionLevels.remove(player.getUuid());
	}
}
