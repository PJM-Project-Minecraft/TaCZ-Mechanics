package ru.liko.tacz_mechanics.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import ru.liko.tacz_mechanics.TaczMechanics;
import ru.liko.tacz_mechanics.client.ClientDistantFireSettings;

/**
 * Pushes server-side distant-fire flags and the muffle range to the client so
 * the {@link ru.liko.tacz_mechanics.client.sound.DistantFireHandler low-pass mixin}
 * has authoritative values without depending on a client-side config.
 */
public record DistantFireSyncPayload(
    boolean enabled,
    int nearSoundRange,
    int maxDistance,
    boolean soundPropagation,
    double soundSpeedBlocksPerSecond,
    int soundPropagationMaxDelayTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<DistantFireSyncPayload> TYPE = new CustomPacketPayload.Type<>(
        ResourceLocation.fromNamespaceAndPath(TaczMechanics.MODID, "distant_fire_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DistantFireSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DistantFireSyncPayload decode(RegistryFriendlyByteBuf buf) {
            return new DistantFireSyncPayload(
                buf.readBoolean(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readBoolean(),
                buf.readDouble(),
                buf.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, DistantFireSyncPayload packet) {
            buf.writeBoolean(packet.enabled);
            buf.writeVarInt(packet.nearSoundRange);
            buf.writeVarInt(packet.maxDistance);
            buf.writeBoolean(packet.soundPropagation);
            buf.writeDouble(packet.soundSpeedBlocksPerSecond);
            buf.writeVarInt(packet.soundPropagationMaxDelayTicks);
        }
    };

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DistantFireSyncPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> ClientDistantFireSettings.applyFromServer(
            packet.enabled,
            packet.nearSoundRange,
            packet.maxDistance,
            packet.soundPropagation,
            packet.soundSpeedBlocksPerSecond,
            packet.soundPropagationMaxDelayTicks));
    }
}
