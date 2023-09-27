package squeek.appleskin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.event.network.CustomPayloadEvent;

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

	public static void handle(final MessageSaturationSync message, CustomPayloadEvent.Context ctx)
	{
		ctx.enqueueWork(() -> {
			NetworkHelper.getSidedPlayer(ctx).getFoodData().setSaturation(message.saturationLevel);
		});
		ctx.setPacketHandled(true);
	}
}