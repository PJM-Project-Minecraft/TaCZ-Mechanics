package ru.liko.tacz_mechanics.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import ru.liko.tacz_mechanics.TaczMechanics;
import ru.liko.tacz_mechanics.client.ClientTweakSettings;

/**
 * Синхронизация серверных твиков UI (скрытие прицела TACZ и хитмаркеров) на клиент.
 */
public record TweaksSyncPayload(boolean hideGunCrosshair, boolean hideHitMarkers) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TweaksSyncPayload> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(TaczMechanics.MODID, "tweaks_sync"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TweaksSyncPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public TweaksSyncPayload decode(RegistryFriendlyByteBuf buf) {
            return new TweaksSyncPayload(buf.readBoolean(), buf.readBoolean());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, TweaksSyncPayload packet) {
            buf.writeBoolean(packet.hideGunCrosshair());
            buf.writeBoolean(packet.hideHitMarkers());
        }
    };

    @Override
    public @NotNull CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TweaksSyncPayload packet, IPayloadContext context) {
        context.enqueueWork(() ->
                ClientTweakSettings.applyFromServer(packet.hideGunCrosshair(), packet.hideHitMarkers()));
    }
}
