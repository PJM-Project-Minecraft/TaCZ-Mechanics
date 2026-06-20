package ru.liko.tacz_mechanics.client.freeaim;

import com.tacz.guns.api.event.common.GunFireEvent;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.LogicalSide;
import net.neoforged.fml.common.EventBusSubscriber;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.TaczMechanics;

/**
 * Feeds a recoil impulse into the sway springs when the local player fires.
 * TaCZ already applies recoil to the camera; this adds a visual gun-model kick.
 */
@EventBusSubscriber(modid = TaczMechanics.MODID, value = Dist.CLIENT)
public final class RecoilSource {

    private RecoilSource() {
    }

    @SubscribeEvent
    public static void onGunFire(GunFireEvent event) {
        if (!Config.FreeAim.enabled || !Config.FreeAim.recoilEnabled) {
            return;
        }
        if (event.getLogicalSide() != LogicalSide.CLIENT) {
            return;
        }
        if (Minecraft.getInstance().player != event.getShooter()) {
            return;
        }
        float scale = (float) Config.FreeAim.recoilScale;
        // Pitch up (negative camera pitch is up) -> push barrel up via positive impulse,
        // small alternating horizontal kick using game time parity for variety.
        float yawKick = (Minecraft.getInstance().player.tickCount % 2 == 0) ? 0.25f : -0.25f;
        FreeAimHandler.getInstance().addRecoilImpulse(scale, yawKick * scale);
    }
}
