package squeek.appleskin.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import squeek.appleskin.ModInfo;

public record MessageNaturalRegenerationSync(boolean naturalRegeneration) implements CustomPacketPayload
{
	public static boolean NATURAL_REGENERATION = true;
	public static final Type<MessageNaturalRegenerationSync> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ModInfo.MODID, "natural_regeneration"));
	public static final StreamCodec<RegistryFriendlyByteBuf, MessageNaturalRegenerationSync> CODEC = StreamCodec.composite(
		ByteBufCodecs.BOOL,
		MessageNaturalRegenerationSync::naturalRegeneration,
		MessageNaturalRegenerationSync::new
	);

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return TYPE;
	}

	public static void handle(final MessageNaturalRegenerationSync message, final IPayloadContext ctx)
	{
		NATURAL_REGENERATION = message.naturalRegeneration();
	}
}