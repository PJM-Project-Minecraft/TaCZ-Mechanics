package ru.liko.tacz_mechanics.client.freeaim;

import com.mojang.logging.LogUtils;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.network.FreeAimSyncPacket;

/**
 * Free Aim orchestrator: spring-driven weapon sway.
 * Holds two SwaySpring axes (pitch/yaw), feeds them impulses from sources
 * (look delta now; movement/recoil added later) and exposes the effective
 * (ADS-scaled) offset to all consumers.
 */
public class FreeAimHandler {

    private static final FreeAimHandler INSTANCE = new FreeAimHandler();

    private final SwaySpring pitchSpring = new SwaySpring();
    private final SwaySpring yawSpring = new SwaySpring();
    private final MovementSource movementSource = new MovementSource();

    // Previous player rotation (for look-delta source)
    private float lastPitch = Float.NaN;
    private float lastYaw = Float.NaN;

    // Pending recoil impulse (added by RecoilSource between ticks)
    private float pendingRecoilPitch = 0f;
    private float pendingRecoilYaw = 0f;

    private int syncTimer = 0;

    public static FreeAimHandler getInstance() {
        return INSTANCE;
    }

    public void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (!Config.FreeAim.enabled || player == null || mc.isPaused() || !isHoldingGun(player)) {
            reset();
            return;
        }

        // Sync spring params from config every tick (cheap, allows live reload)
        float stiffness = (float) Config.FreeAim.stiffness;
        float damping = (float) Config.FreeAim.damping;
        float max = (float) Config.FreeAim.maxAngle;
        pitchSpring.setParams(stiffness, damping, max);
        yawSpring.setParams(stiffness, damping, max);

        float currentPitch = player.getXRot();
        float currentYaw = player.getYRot();

        if (Float.isNaN(lastPitch)) {
            lastPitch = currentPitch;
            lastYaw = currentYaw;
        }

        // === Source: look delta ===
        float deltaPitch = currentPitch - lastPitch;
        float deltaYaw = currentYaw - lastYaw;
        while (deltaYaw > 180) deltaYaw -= 360;
        while (deltaYaw < -180) deltaYaw += 360;
        lastPitch = currentPitch;
        lastYaw = currentYaw;

        float lookSens = (float) Config.FreeAim.lookSensitivity;
        // Gun lags behind: impulse opposite to camera movement
        pitchSpring.addImpulse(-deltaPitch * lookSens);
        yawSpring.addImpulse(-deltaYaw * lookSens);

        // === Source: movement ===
        movementSource.apply(player, pitchSpring, yawSpring);

        // === Source: recoil (queued by RecoilSource) ===
        if (pendingRecoilPitch != 0f || pendingRecoilYaw != 0f) {
            pitchSpring.addImpulse(pendingRecoilPitch);
            yawSpring.addImpulse(pendingRecoilYaw);
            pendingRecoilPitch = 0f;
            pendingRecoilYaw = 0f;
        }

        // === Integrate (dt = 1 tick) ===
        pitchSpring.update(1f);
        yawSpring.update(1f);

        // === Sync effective offset (ADS already applied) to server every 2 ticks ===
        if (++syncTimer >= 2) {
            syncTimer = 0;
            float effPitch = getEffectivePitch(1f);
            float effYaw = getEffectiveYaw(1f);
            if (effPitch != 0f || effYaw != 0f) {
                try {
                    PacketDistributor.sendToServer(new FreeAimSyncPacket(effPitch, effYaw));
                } catch (Exception e) {
                    LogUtils.getLogger().warn("Failed to send FreeAim sync packet", e);
                }
            }
        }
    }

    public void addRecoilImpulse(float pitchImpulse, float yawImpulse) {
        pendingRecoilPitch += pitchImpulse;
        pendingRecoilYaw += yawImpulse;
    }

    private boolean isHoldingGun(LocalPlayer player) {
        try {
            return IGun.mainHandHoldGun(player);
        } catch (Exception e) {
            return false;
        }
    }

    private float aimingProgress(float pt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0f;
        try {
            return IClientPlayerGunOperator.fromLocalPlayer(mc.player).getClientAimingProgress(pt);
        } catch (Exception e) {
            return 0f;
        }
    }

    /** ADS scale: lerp(1.0, adsMultiplier, aimingProgress). */
    private float adsFactor(float pt) {
        float ads = (float) Config.FreeAim.adsMultiplier;
        float p = aimingProgress(pt);
        return 1f + (ads - 1f) * p;
    }

    public float getEffectivePitch(float pt) {
        return pitchSpring.getInterpolated(pt) * adsFactor(pt);
    }

    public float getEffectiveYaw(float pt) {
        return yawSpring.getInterpolated(pt) * adsFactor(pt);
    }

    public float getCrosshairX(float pt) {
        if (!isActive() || Config.FreeAim.disableCrosshairMovement) return 0f;
        return -getEffectiveYaw(pt) * (float) Config.FreeAim.crosshairScale;
    }

    public float getCrosshairY(float pt) {
        if (!isActive() || Config.FreeAim.disableCrosshairMovement) return 0f;
        return -getEffectivePitch(pt) * (float) Config.FreeAim.crosshairScale;
    }

    public boolean isActive() {
        if (!Config.FreeAim.enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        return player != null && isHoldingGun(player);
    }

    public void reset() {
        pitchSpring.reset();
        yawSpring.reset();
        lastPitch = Float.NaN;
        lastYaw = Float.NaN;
        pendingRecoilPitch = 0f;
        pendingRecoilYaw = 0f;
        syncTimer = 0;
        movementSource.reset();
    }
}
