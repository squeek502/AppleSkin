package squeek.appleskin.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record SaturationSyncPayload(float saturation) implements CustomPacketPayload
{
	public static final StreamCodec<FriendlyByteBuf, SaturationSyncPayload> CODEC = CustomPacketPayload.codec(SaturationSyncPayload::write, SaturationSyncPayload::new);
	public static final CustomPacketPayload.Type<SaturationSyncPayload> ID = new Type<>(Identifier.fromNamespaceAndPath("appleskin", "saturation"));

	public SaturationSyncPayload(FriendlyByteBuf buf)
	{
		this(buf.readFloat());
	}

	public void write(FriendlyByteBuf buf)
	{
		buf.writeFloat(saturation);
	}

	public float getSaturation()
	{
		return saturation;
	}

	@Override
	public Type<? extends CustomPacketPayload> type()
	{
		return ID;
	}
}
