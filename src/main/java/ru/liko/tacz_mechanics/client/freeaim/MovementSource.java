package ru.liko.tacz_mechanics.client.freeaim;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import ru.liko.tacz_mechanics.Config;

/**
 * Produces walk/sprint bob and jump/land kicks as spring impulses.
 * Stateful: tracks bob phase and previous ground state.
 */
public final class MovementSource {

    private float bobPhase = 0f;
    private boolean wasOnGround = true;

    public void apply(LocalPlayer player, SwaySpring pitchSpring, SwaySpring yawSpring) {
        if (!Config.FreeAim.movementEnabled) {
            return;
        }

        // Horizontal speed (blocks/tick) from this tick's movement
        double dx = player.getX() - player.xo;
        double dz = player.getZ() - player.zo;
        float speed = (float) Math.sqrt(dx * dx + dz * dz);

        // Walk/sprint bob: advance phase by speed, emit a gentle figure-eight sway
        if (speed > 0.005f && player.onGround()) {
            float amp = player.isSprinting()
                    ? (float) Config.FreeAim.movementSprintScale
                    : (float) Config.FreeAim.movementWalkScale;
            bobPhase += speed * 8f;
            // Vertical bob twice per horizontal cycle, horizontal once
            pitchSpring.addImpulse(Mth.sin(bobPhase * 2f) * amp * 0.05f);
            yawSpring.addImpulse(Mth.cos(bobPhase) * amp * 0.05f);
        }

        // Jump / land kick
        boolean onGround = player.onGround();
        if (wasOnGround && !onGround) {
            // Just left ground (jumped): barrel dips
            pitchSpring.addImpulse(-(float) Config.FreeAim.movementJumpScale);
        } else if (!wasOnGround && onGround) {
            // Just landed: barrel kicks up
            pitchSpring.addImpulse((float) Config.FreeAim.movementJumpScale);
        }
        wasOnGround = onGround;
    }

    public void reset() {
        bobPhase = 0f;
        wasOnGround = true;
    }
}
