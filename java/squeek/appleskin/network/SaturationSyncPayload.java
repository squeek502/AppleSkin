package squeek.appleskin.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import squeek.appleskin.AppleSkin;

public record SaturationSyncPayload(float saturation) implements CustomPayload
{
	public static final PacketCodec<PacketByteBuf, SaturationSyncPayload> CODEC = CustomPayload.codecOf(SaturationSyncPayload::write, SaturationSyncPayload::new);
	public static final CustomPayload.Id<SaturationSyncPayload> ID = new Id<>(Identifier.of(AppleSkin.MOD_ID, "saturation"));

	public SaturationSyncPayload(PacketByteBuf buf)
	{
		this(buf.readFloat());
	}

	public void write(PacketByteBuf buf)
	{
		buf.writeFloat(saturation);
	}

	public float getSaturation()
	{
		return saturation;
	}

	@Override
	public Id<? extends CustomPayload> getId()
	{
		return ID;
	}
}
