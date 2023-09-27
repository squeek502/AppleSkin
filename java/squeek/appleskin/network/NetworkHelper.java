package squeek.appleskin.network;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.network.CustomPayloadEvent;
import net.minecraftforge.network.NetworkDirection;

public class NetworkHelper
{
	public static Player getSidedPlayer(CustomPayloadEvent.Context ctx)
	{
		return ctx.getDirection() == NetworkDirection.PLAY_TO_SERVER ? ctx.getSender() : Minecraft.getInstance().player;
	}
}