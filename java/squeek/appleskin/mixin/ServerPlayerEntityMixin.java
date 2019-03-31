package squeek.appleskin.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import squeek.appleskin.network.SyncHandler;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends Entity
{
	public ServerPlayerEntityMixin(EntityType<?> entityType, World world)
	{
		super(entityType, world);
	}

	@Inject(at = @At("HEAD"), method = "tick")
	void onUpdate(CallbackInfo info)
	{
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		SyncHandler.onPlayerUpdate(player);
	}
}
