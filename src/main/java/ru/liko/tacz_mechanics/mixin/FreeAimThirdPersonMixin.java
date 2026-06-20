package ru.liko.tacz_mechanics.mixin;

import com.tacz.guns.client.animation.third.InnerThirdPersonManager;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.client.freeaim.FreeAimClientCache;

/**
 * Applies free-aim sway to the third-person gun pose of any player,
 * using their broadcast offset from FreeAimClientCache.
 */
@Mixin(value = InnerThirdPersonManager.class, remap = false)
public class FreeAimThirdPersonMixin {

    @Inject(method = "setRotationAnglesHead", at = @At("TAIL"), require = 0)
    private static void taczMechanics$applyFreeAim(LivingEntity entityIn, ModelPart rightArm, ModelPart leftArm,
                                                   ModelPart body, ModelPart head, float limbSwingAmount,
                                                   CallbackInfo ci) {
        if (!Config.FreeAim.enabled || !Config.FreeAim.thirdPersonEnabled || entityIn == null) {
            return;
        }
        UUID uuid = entityIn.getUUID();
        float pitch = FreeAimClientCache.getPitch(uuid);
        float yaw = FreeAimClientCache.getYaw(uuid);
        if (Math.abs(pitch) < 0.001f && Math.abs(yaw) < 0.001f) {
            return;
        }
        // Convert degrees to radians; tilt right arm (gun) and body slightly.
        float pitchRad = (float) Math.toRadians(pitch);
        float yawRad = (float) Math.toRadians(yaw);
        rightArm.xRot += -pitchRad;
        rightArm.yRot += yawRad;
        body.yRot += yawRad * 0.3f;
    }
}
