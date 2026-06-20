package ru.liko.tacz_mechanics.movement;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import ru.liko.tacz_mechanics.Config;

/**
 * Helpers shared by the leaning (probe) movement. The sit/crawl postures were removed;
 * the pose view offset is now always zero, and leaning is applied directly in the
 * camera/eye mixins.
 */
public final class MovementPosture {

    private static final Logger LOGGER = LogUtils.getLogger();

    private MovementPosture() {
    }

    /**
     * Pose-based view offset. Sit/crawl были удалены, поэтому позового смещения больше нет —
     * наклон влево/вправо применяется отдельно в миксинах камеры/глаз.
     */
    public static Vec3 cameraEyeOffset(Player player, PlayerState state, float partialTick) {
        return Vec3.ZERO;
    }

    /**
     * Дебаг-лог фактического хитбокса игрока и точки обзора. Включается флагом {@code movement.debug}.
     * {@code side} — "CLIENT" или "SERVER".
     */
    public static void logHitbox(String side, Player player, PlayerState state) {
        if (!Config.Movement.debug) {
            return;
        }
        AABB box = player.getBoundingBox();
        Vec3 eye = player.getEyePosition();
        LOGGER.info(String.format(
            "[MovementDebug/%s] probe=%d pos=(%.3f, %.3f, %.3f) bbW=%.3f bbH=%.3f eyeH=%.3f "
                + "box=[x %.3f..%.3f | y %.3f..%.3f | z %.3f..%.3f] eyePos=(%.3f, %.3f, %.3f) yBodyRot=%.1f",
            side, state == null ? 0 : state.getProbe(),
            player.getX(), player.getY(), player.getZ(),
            player.getBbWidth(), player.getBbHeight(), player.getEyeHeight(),
            box.minX, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ,
            eye.x, eye.y, eye.z, player.yBodyRot));
    }

    /**
     * Проверка перехода между закодированными состояниями. Sit/crawl убраны, остаётся только
     * наклон (probe), для которого переходы не требуют проверки места — всегда разрешено.
     */
    public static boolean canApplyMovementCode(Player player, int oldCode, int newCode) {
        return true;
    }
}
