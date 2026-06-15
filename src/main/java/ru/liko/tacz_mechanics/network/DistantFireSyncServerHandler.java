package ru.liko.tacz_mechanics.network;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.TaczMechanics;

@EventBusSubscriber(modid = TaczMechanics.MODID)
public final class DistantFireSyncServerHandler {

    private DistantFireSyncServerHandler() {
    }

    public static void sendTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, currentPayload());
    }

    public static DistantFireSyncPayload currentPayload() {
        return new DistantFireSyncPayload(
            Config.DistantFire.enabled,
            Config.DistantFire.nearSoundRange,
            Config.DistantFire.maxDistance,
            Config.DistantFire.soundPropagation,
            Config.DistantFire.soundSpeedBlocksPerSecond,
            Config.DistantFire.soundPropagationMaxDelayTicks);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.level().isClientSide()) {
            return;
        }
        sendTo(sp);
    }

    @SubscribeEvent
    public static void onServerConfigLoaded(ModConfigEvent event) {
        if (event instanceof ModConfigEvent.Unloading) {
            return;
        }
        if (event.getConfig().getSpec() != Config.SERVER_SPEC) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        DistantFireSyncPayload payload = currentPayload();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer(p, payload);
        }
    }
}
