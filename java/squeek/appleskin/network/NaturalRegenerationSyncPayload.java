package squeek.appleskin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record NaturalRegenerationSyncPayload(boolean naturalRegeneration) implements CustomPacketPayload
{
	public static final StreamCodec<FriendlyByteBuf, NaturalRegenerationSyncPayload> CODEC = CustomPacketPayload.codec(NaturalRegenerationSyncPayload::write, NaturalRegenerationSyncPayload::new);
	public static final CustomPacketPayload.Type<NaturalRegenerationSyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("appleskin", "natural_regeneration"));

	public NaturalRegenerationSyncPayload(FriendlyByteBuf buf)
	{
		this(buf.readBoolean());
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeBoolean(naturalRegeneration);
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
