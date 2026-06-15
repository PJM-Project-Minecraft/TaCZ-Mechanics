package ru.liko.tacz_mechanics.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import ru.liko.tacz_mechanics.TaczMechanics;

import java.util.Optional;

/**
 * Server-resolved distant-fire shot. The server already looked up the caliber and
 * computed the crossfade so the client just plays {@code primary} (and optionally
 * {@code secondary}) at the given volumes. {@code soundRange} is in blocks and is
 * passed straight to {@link net.minecraft.sounds.SoundEvent#createFixedRangeEvent}.
 */
public record DistantFireSoundPacket(
    double x,
    double y,
    double z,
    float pitch,
    ResourceLocation primarySound,
    float primaryVolume,
    Optional<ResourceLocation> secondarySound,
    float secondaryVolume,
    float soundRange
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DistantFireSoundPacket> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(TaczMechanics.MODID, "distant_fire_sound")
    );

    public static final StreamCodec<RegistryFriendlyByteBuf, DistantFireSoundPacket> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DistantFireSoundPacket decode(RegistryFriendlyByteBuf buf) {
            double x = buf.readDouble();
            double y = buf.readDouble();
            double z = buf.readDouble();
            float pitch = buf.readFloat();
            ResourceLocation primary = buf.readResourceLocation();
            float primaryVol = buf.readFloat();
            boolean hasSecondary = buf.readBoolean();
            Optional<ResourceLocation> secondary = hasSecondary ? Optional.of(buf.readResourceLocation()) : Optional.empty();
            float secondaryVol = hasSecondary ? buf.readFloat() : 0f;
            float range = buf.readFloat();
            return new DistantFireSoundPacket(x, y, z, pitch, primary, primaryVol, secondary, secondaryVol, range);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, DistantFireSoundPacket packet) {
            buf.writeDouble(packet.x);
            buf.writeDouble(packet.y);
            buf.writeDouble(packet.z);
            buf.writeFloat(packet.pitch);
            buf.writeResourceLocation(packet.primarySound);
            buf.writeFloat(packet.primaryVolume);
            buf.writeBoolean(packet.secondarySound.isPresent());
            if (packet.secondarySound.isPresent()) {
                buf.writeResourceLocation(packet.secondarySound.get());
                buf.writeFloat(packet.secondaryVolume);
            }
            buf.writeFloat(packet.soundRange);
        }
    };

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DistantFireSoundPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> ru.liko.tacz_mechanics.client.sound.DistantFireClientHandler.handleDistantFireSound(packet));
    }
}
