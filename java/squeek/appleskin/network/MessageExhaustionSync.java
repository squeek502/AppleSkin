package squeek.appleskin.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import squeek.appleskin.helpers.HungerHelper;

public class MessageExhaustionSync implements IMessage, IMessageHandler<MessageExhaustionSync, IMessage>
{
	float exhaustionLevel;

	public MessageExhaustionSync()
	{
	}

	public MessageExhaustionSync(float exhaustionLevel)
	{
		this.exhaustionLevel = exhaustionLevel;
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeFloat(exhaustionLevel);
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		exhaustionLevel = buf.readFloat();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IMessage onMessage(final MessageExhaustionSync message, final MessageContext ctx)
	{
		// defer to the next game loop; we can't guarantee that Minecraft.thePlayer is initialized yet
		Minecraft.getMinecraft().addScheduledTask(new Runnable() {
			@Override
			public void run() {
				HungerHelper.setExhaustion(NetworkHelper.getSidedPlayer(ctx), message.exhaustionLevel);
			}
		});
		return null;
	}
}