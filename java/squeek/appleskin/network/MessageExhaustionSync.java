package squeek.appleskin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageExhaustionSync
{
	float exhaustionLevel;

	public MessageExhaustionSync(float exhaustionLevel)
	{
		this.exhaustionLevel = exhaustionLevel;
	}

	public static void encode(MessageExhaustionSync pkt, FriendlyByteBuf buf)
	{
		buf.writeFloat(pkt.exhaustionLevel);
	}

	public static MessageExhaustionSync decode(FriendlyByteBuf buf)
	{
		return new MessageExhaustionSync(buf.readFloat());
	}

	public static void handle(final MessageExhaustionSync message, Supplier<NetworkEvent.Context> ctx)
	{
		ctx.get().enqueueWork(() -> {
			NetworkHelper.getSidedPlayer(ctx.get()).getFoodData().setExhaustion(message.exhaustionLevel);
		});
		ctx.get().setPacketHandled(true);
	}
}