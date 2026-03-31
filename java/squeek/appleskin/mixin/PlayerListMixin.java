package squeek.appleskin.mixin;

import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.network.SyncHandler;

@Mixin(PlayerList.class)
public class PlayerListMixin
{
	@Inject(at = @At("TAIL"), method = "placeNewPlayer")
	private void onPlayerConnect(Connection conn, ServerPlayer player, CommonListenerCookie clientData, CallbackInfo info)
	{
		SyncHandler.onPlayerLoggedIn(player);
	}
}
