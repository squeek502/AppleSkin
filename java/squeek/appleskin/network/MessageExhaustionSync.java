package squeek.appleskin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import squeek.appleskin.ModInfo;

public record MessageExhaustionSync(float exhaustionLevel) implements CustomPacketPayload
{
	public static final Type<MessageExhaustionSync> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModInfo.MODID, "exhaustion"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MessageExhaustionSync> CODEC = StreamCodec.composite(
		ByteBufCodecs.FLOAT,
		MessageExhaustionSync::exhaustionLevel,
		MessageExhaustionSync::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(final MessageExhaustionSync message, final IPayloadContext ctx)
	{
		ctx.enqueueWork(() -> {
			ctx.player().getFoodData().exhaustionLevel = message.exhaustionLevel();
		});
	}
}