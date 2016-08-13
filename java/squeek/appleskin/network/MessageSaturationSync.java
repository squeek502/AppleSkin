package squeek.appleskin.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageSaturationSync implements IMessage, IMessageHandler<MessageSaturationSync, IMessage>
{
	float saturationLevel;

	public MessageSaturationSync()
	{
	}

	public MessageSaturationSync(float saturationLevel)
	{
		this.saturationLevel = saturationLevel;
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeFloat(saturationLevel);
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		saturationLevel = buf.readFloat();
	}

	@Override
	public IMessage onMessage(final MessageSaturationSync message, final MessageContext ctx)
	{
		// defer to the next game loop; we can't guarantee that Minecraft.thePlayer is initialized yet
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			@Override
			public void run() {
				NetworkHelper.getSidedPlayer(ctx).getFoodStats().setFoodSaturationLevel(message.saturationLevel);
			}
		});
		return null;
	}
}