package squeek.appleskin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;

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

	public static void handle(final MessageExhaustionSync message, NetworkEvent.Context ctx)
	{
		ctx.enqueueWork(() -> {
			NetworkHelper.getSidedPlayer(ctx).getFoodData().setExhaustion(message.exhaustionLevel);
		});
		ctx.setPacketHandled(true);
	}
}