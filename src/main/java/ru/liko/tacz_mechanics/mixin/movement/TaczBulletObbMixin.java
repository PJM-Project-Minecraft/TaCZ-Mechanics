package ru.liko.tacz_mechanics.mixin.movement;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.EntityUtil;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.movement.CrawlObb;
import ru.liko.tacz_mechanics.movement.MovementStateManager;
import ru.liko.tacz_mechanics.movement.PlayerState;

/**
 * OBB-уточнение попадания пули в ЛЕЖАЩЕГО игрока.
 *
 * <p>TaCZ ищет цель по осевому AABB ({@code EntityUtil.getHitResult} → {@code AABB.clip}).
 * Лёжа этот AABB — «обёртка» повёрнутого тела и включает пустые углы. Здесь, если AABB задело,
 * но луч прошёл мимо самого тонкого повёрнутого тела ({@link CrawlObb}), возвращаем {@code null}.
 * Тогда {@code findEntityOnPath} не считает это попаданием, и пуля летит дальше (без {@code discard}).
 * Так пуля бьёт по реальному силуэту лёжа под любым углом, а коллизия движения остаётся обычным AABB.</p>
 */
@Mixin(value = EntityUtil.class, remap = false)
public class TaczBulletObbMixin {

    @Inject(method = "getHitResult", at = @At("RETURN"), cancellable = true)
    private static void taczMechanics$obbRefineCrawl(Projectile bullet, Entity target, Vec3 start, Vec3 end,
                                                     CallbackInfoReturnable<EntityKineticBullet.EntityResult> cir) {
        if (!Config.Movement.enabled) return;
        if (cir.getReturnValue() == null) return; // AABB и так мимо — ничего не меняем

        if (!(target instanceof Player player)) return;
        PlayerState state = MovementStateManager.get(player.getUUID());
        if (state == null || !state.isCrawling()) return;

        // AABB задело, но проверяем тонкое повёрнутое тело: луч в пустом углу — не попадание.
        if (!CrawlObb.segmentHitsBody(player, start, end)) {
            cir.setReturnValue(null);
        }
    }
}
