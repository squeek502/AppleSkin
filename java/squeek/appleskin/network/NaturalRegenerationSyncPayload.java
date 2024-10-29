package squeek.appleskin.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record NaturalRegenerationSyncPayload(boolean naturalRegeneration) implements CustomPayload
{
	public static final PacketCodec<PacketByteBuf, NaturalRegenerationSyncPayload> CODEC = CustomPayload.codecOf(NaturalRegenerationSyncPayload::write, NaturalRegenerationSyncPayload::new);
	public static final CustomPayload.Id<NaturalRegenerationSyncPayload> ID = new Id<>(Identifier.of("appleskin", "natural_regeneration"));

	public NaturalRegenerationSyncPayload(PacketByteBuf buf)
	{
		this(buf.readBoolean());
	}

	public void write(PacketByteBuf buf)
	{
		buf.writeBoolean(naturalRegeneration);
	}

	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
}
