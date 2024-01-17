package squeek.appleskin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import squeek.appleskin.ModInfo;

public record MessageSaturationSync(float saturationLevel) implements CustomPacketPayload
{
	public static final ResourceLocation ID = new ResourceLocation(ModInfo.MODID, "saturation");

	public MessageSaturationSync(final FriendlyByteBuf buffer)
	{
		this(buffer.readFloat());
	}

	@Override
	public void write(final FriendlyByteBuf buffer)
	{
		buffer.writeFloat(saturationLevel());
	}

	@Override
	public ResourceLocation id()
	{
		return ID;
	}

	public static void handle(final MessageSaturationSync message, final PlayPayloadContext ctx)
	{
		ctx.workHandler().submitAsync(() -> {
			ctx.player().ifPresent(player -> player.getFoodData().setSaturation(message.saturationLevel()));
		});
	}
}