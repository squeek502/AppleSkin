package squeek.appleskin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;

public class NetworkHelper
{
	public static PlayerEntity getSidedPlayer(NetworkEvent.Context ctx)
	{
		return ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER ? ctx.getSender() : Minecraft.getInstance().player;
	}
}