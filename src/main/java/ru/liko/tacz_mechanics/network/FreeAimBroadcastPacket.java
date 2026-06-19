package ru.liko.tacz_mechanics.network;

import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import ru.liko.tacz_mechanics.TaczMechanics;
import ru.liko.tacz_mechanics.client.freeaim.FreeAimClientCache;

import java.util.UUID;

/**
 * Server -> tracking clients: broadcasts a player's effective free-aim offset
 * so observers can render third-person gun sway.
 */
public record FreeAimBroadcastPacket(UUID playerId, float pitch, float yaw) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FreeAimBroadcastPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TaczMechanics.MODID, "free_aim_broadcast"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FreeAimBroadcastPacket> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, FreeAimBroadcastPacket::playerId,
            ByteBufCodecs.FLOAT, FreeAimBroadcastPacket::pitch,
            ByteBufCodecs.FLOAT, FreeAimBroadcastPacket::yaw,
            FreeAimBroadcastPacket::new);

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FreeAimBroadcastPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> FreeAimClientCache.update(packet.playerId(), packet.pitch(), packet.yaw()));
    }
}
