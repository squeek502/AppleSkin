package squeek.appleskin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.NetworkEvent;

import java.util.function.Supplier;

public class MessageSaturationSync
{
	float saturationLevel;

	public MessageSaturationSync(float saturationLevel)
	{
		this.saturationLevel = saturationLevel;
	}

	public static void encode(MessageSaturationSync pkt, FriendlyByteBuf buf)
	{
		buf.writeFloat(pkt.saturationLevel);
	}

	public static MessageSaturationSync decode(FriendlyByteBuf buf)
	{
		return new MessageSaturationSync(buf.readFloat());
	}

	public static void handle(final MessageSaturationSync message, NetworkEvent.Context ctx)
	{
		ctx.enqueueWork(() -> {
			NetworkHelper.getSidedPlayer(ctx).getFoodData().setSaturation(message.saturationLevel);
		});
		ctx.setPacketHandled(true);
	}
}