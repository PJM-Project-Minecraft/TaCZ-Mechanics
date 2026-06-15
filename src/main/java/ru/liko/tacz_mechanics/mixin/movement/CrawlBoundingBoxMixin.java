package ru.liko.tacz_mechanics.mixin.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.liko.tacz_mechanics.Config;
import ru.liko.tacz_mechanics.movement.MovementPosture;
import ru.liko.tacz_mechanics.movement.MovementStateManager;
import ru.liko.tacz_mechanics.movement.PlayerState;

/**
 * Прямоугольный хитбокс для позы ЛЁЖА: длинный вдоль тела, узкий поперёк.
 *
 * <p>{@link net.minecraft.world.entity.EntityDimensions} умеет только квадрат (width×width),
 * поэтому AABB строим сами. Перехватываем {@code Entity#makeBoundingBox} — единственный
 * чокпоинт, через который движок создаёт бокс из позиции (его зовут {@code setPos} и
 * {@code refreshDimensions}). Значит прямоугольник работает и для коллизий движения, и для
 * попаданий пуль ({@code getBoundingBox}), и на клиенте, и на сервере.</p>
 *
 * <p>Прямоугольник повёрнут под тело через осево-выровненные полугабариты: длинная ось тела
 * — {@code (-sin yaw, cos yaw)}, поперечная — {@code (cos yaw, sin yaw)}. Бокс всегда накрывает
 * повёрнутое тело: по осям он 2.0×0.6, по диагонали (~45°) ужимается к ~1.46×1.46, но тело
 * не оголяет. Пересчёт каждый тик (в обработчиках уже зовётся {@code refreshDimensions}).</p>
 */
@Mixin(Entity.class)
public abstract class CrawlBoundingBoxMixin {

    @Inject(method = "makeBoundingBox", at = @At("HEAD"), cancellable = true)
    private void taczMechanics$crawlBoundingBox(CallbackInfoReturnable<AABB> cir) {
        if (!Config.Movement.enabled) return;

        Entity self = (Entity) (Object) this;
        if (!(self instanceof Player player)) return;

        PlayerState state = MovementStateManager.get(player.getUUID());
        if (state == null || !state.isCrawling()) return;

        double halfLen = MovementPosture.CRAWL_LENGTH / 2.0;
        double halfWid = MovementPosture.CRAWL_WIDTH / 2.0;

        float yRad = player.yBodyRot * Mth.DEG_TO_RAD;
        double s = Math.abs(Mth.sin(yRad));
        double c = Math.abs(Mth.cos(yRad));

        // Осевая «обёртка» повёрнутого тела: служит и коллизией движения, и broadphase
        // для пуль (узкий OBB-тест делает CrawlObb в миксине попаданий TaCZ).
        double halfX = halfLen * s + halfWid * c;
        double halfZ = halfLen * c + halfWid * s;

        Vec3 pos = player.position();
        double[] center = MovementPosture.crawlCenterXZ(pos.x, pos.z, player.yBodyRot);
        double cx = center[0];
        double cz = center[1];
        cir.setReturnValue(new AABB(
            cx - halfX, pos.y, cz - halfZ,
            cx + halfX, pos.y + MovementPosture.CRAWL_HEIGHT, cz + halfZ
        ));
    }
}
