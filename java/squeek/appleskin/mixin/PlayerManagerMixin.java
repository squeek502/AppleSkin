package squeek.appleskin.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.network.SyncHandler;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin
{
	@Inject(at = @At("TAIL"), method = "onPlayerConnect")
	private void onPlayerConnect(ClientConnection conn, ServerPlayerEntity player, CallbackInfo info)
	{
		SyncHandler.onPlayerLoggedIn(player);
	}
}
