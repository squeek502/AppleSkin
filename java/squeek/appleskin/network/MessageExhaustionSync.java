package squeek.appleskin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import squeek.appleskin.ModInfo;

public record MessageExhaustionSync(float exhaustionLevel) implements CustomPacketPayload
{
	public static final ResourceLocation ID = new ResourceLocation(ModInfo.MODID, "exhaustion");

	public MessageExhaustionSync(final FriendlyByteBuf buffer)
	{
		this(buffer.readFloat());
	}

	@Override
	public void write(final FriendlyByteBuf buffer)
	{
		buffer.writeFloat(exhaustionLevel());
	}

	@Override
	public ResourceLocation id()
	{
		return ID;
	}

	public static void handle(final MessageExhaustionSync message, final PlayPayloadContext ctx)
	{
		ctx.workHandler().submitAsync(() -> {
			ctx.player().ifPresent(player -> player.getFoodData().setExhaustion(message.exhaustionLevel()));
		});
	}
}