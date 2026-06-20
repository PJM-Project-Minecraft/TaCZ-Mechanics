package ru.liko.tacz_mechanics.mixin.movement;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.movement.LeanCollision;
import ru.liko.tacz_mechanics.movement.MovementStateManager;
import ru.liko.tacz_mechanics.movement.PlayerState;

/**
 * Mixin to modify player eye position based on movement state (leaning).
 * This affects raycasting/shooting calculations.
 */
@Mixin(Entity.class)
public abstract class PlayerEyePositionMixin {
    
    @Inject(method = "getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void modifyEyePosition(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        applyPoseEyeOffset(cir, partialTicks);
    }

    @Inject(method = "getEyePosition()Lnet/minecraft/world/phys/Vec3;", at = @At("RETURN"), cancellable = true)
    private void modifyEyePositionNoPartial(CallbackInfoReturnable<Vec3> cir) {
        applyPoseEyeOffset(cir, 1.0f);
    }

    @Unique
    private void applyPoseEyeOffset(CallbackInfoReturnable<Vec3> cir, float partialTicks) {
        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;
        if (!Config.Movement.enabled) return;

        PlayerState state = MovementStateManager.get(player.getUUID());
        if (state == null) return;

        Vec3 original = cir.getReturnValue();
        double offsetX = 0, offsetY = 0, offsetZ = 0;

        // Apply horizontal offset for leaning (clamp to collision-safe magnitude)
        float probeOffset = LeanCollision.safeProbeOffset(player, state.getProbeOffset());
        if (probeOffset != 0) {
            float yaw = player.getYRot();
            double radians = Math.toRadians(yaw);
            offsetX += -probeOffset * LeanCollision.OFFSET_SCALE * Math.cos(radians);
            offsetZ += -probeOffset * LeanCollision.OFFSET_SCALE * Math.sin(radians);
        }

        if (offsetX != 0 || offsetY != 0 || offsetZ != 0) {
            cir.setReturnValue(original.add(offsetX, offsetY, offsetZ));
        }
    }
}
