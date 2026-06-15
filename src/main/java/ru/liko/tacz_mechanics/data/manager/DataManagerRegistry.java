package ru.liko.tacz_mechanics.data.manager;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import ru.liko.tacz_mechanics.TaczMechanics;

@EventBusSubscriber(modid = TaczMechanics.MODID)
public class DataManagerRegistry {

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        var server = event.getServer();
        for (var level : server.getAllLevels()) {
            BulletParticlesManager.INSTANCE.onLevelTick(level);
        }
    }
}
