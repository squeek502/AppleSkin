package squeek.appleskin.network;

import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;
import squeek.appleskin.helpers.HungerHelper;

import java.util.function.Supplier;

public class MessageExhaustionSync
{
	float exhaustionLevel;

	public MessageExhaustionSync(float exhaustionLevel)
	{
		this.exhaustionLevel = exhaustionLevel;
	}

	public static void encode(MessageExhaustionSync pkt, PacketBuffer buf)
	{
		buf.writeFloat(pkt.exhaustionLevel);
	}

	public static MessageExhaustionSync decode(PacketBuffer buf)
	{
		return new MessageExhaustionSync(buf.readFloat());
	}

	@OnlyIn(Dist.CLIENT)
	public static void handle(final MessageExhaustionSync message, Supplier<NetworkEvent.Context> ctx)
	{
		ctx.get().enqueueWork(() -> {
			HungerHelper.setExhaustion(NetworkHelper.getSidedPlayer(ctx.get()), message.exhaustionLevel);
		});
		ctx.get().setPacketHandled(true);
	}
}