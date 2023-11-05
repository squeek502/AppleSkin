package squeek.appleskin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.NetworkEvent;
import net.neoforged.neoforge.network.PlayNetworkDirection;

public class NetworkHelper
{
	public static Player getSidedPlayer(NetworkEvent.Context ctx)
	{
		return ctx.getDirection() == PlayNetworkDirection.PLAY_TO_SERVER ? ctx.getSender() : Minecraft.getInstance().player;
	}
}