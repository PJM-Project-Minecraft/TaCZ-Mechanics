package ru.liko.tacz_mechanics.mixin.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.movement.MovementPosture;
import ru.liko.tacz_mechanics.movement.MovementStateManager;
import ru.liko.tacz_mechanics.movement.PlayerState;

/**
 * Single channel for sit/crawl hitbox sizing.
 *
 * <p>Sit and crawl do not change vanilla {@link Pose} — the player stays in {@link Pose#STANDING},
 * so the override fires for whatever pose the engine asks about. The bounding box is built
 * symmetrically around the player center; no extra translation/rotation is applied (that was
 * the source of the misaligned hitbox).</p>
 */
@Mixin(Player.class)
public abstract class PlayerDimensionsMixin {

    @Inject(method = "getDefaultDimensions", at = @At("RETURN"), cancellable = true)
    private void taczMechanics$applyMovementDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        if (!Config.Movement.enabled) return;

        Player player = (Player) (Object) this;
        PlayerState state = MovementStateManager.get(player.getUUID());
        if (state == null) return;

        if (state.isCrawling()) {
            float h = MovementPosture.CRAWL_HEIGHT;
            float eye = Mth.clamp(MovementPosture.CRAWL_EYE_HEIGHT, 0.08f, h - 0.06f);
            cir.setReturnValue(EntityDimensions.scalable(MovementPosture.CRAWL_WIDTH, h).withEyeHeight(eye));
        } else if (state.isSitting()) {
            float h = MovementPosture.SIT_HEIGHT;
            float eye = Mth.clamp(MovementPosture.SIT_EYE_HEIGHT, 0.1f, h - 0.06f);
            cir.setReturnValue(EntityDimensions.scalable(MovementPosture.SIT_WIDTH, h).withEyeHeight(eye));
        }
    }
}
