package ru.liko.tacz_mechanics.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.particles.BulletHoleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Заменяет спавн дыр от пуль на вариант с force=true, чтобы они гарантированно
 * отображались (в т.ч. на блоке, в который попадает пуля после пробития).
 */
@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class BulletHoleForceSpawnMixin {

    @Redirect(
        method = "onHitBlock",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/level/ServerLevel;sendParticles(Lnet/minecraft/core/particles/ParticleOptions;DDDIDDDD)I",
            remap = true
        )
    )
    private int taczMechanics$forceBulletHoleSpawn(
        ServerLevel level,
        ParticleOptions options,
        double x, double y, double z,
        int count,
        double xOff, double yOff, double zOff, double speed
    ) {
        if (options instanceof BulletHoleOption) {
            for (var player : level.players()) {
                level.sendParticles(player, options, true, x, y, z, count, xOff, yOff, zOff, speed);
            }
            return count;
        }
        return level.sendParticles(options, x, y, z, count, xOff, yOff, zOff, speed);
    }
}
