package ru.liko.tacz_mechanics.movement;

import com.mojang.logging.LogUtils;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import ru.liko.tacz_mechanics.Config;

/**
 * Коллизии и проверки для поз «сидя» и «ползком» (без наклона влево/вправо).
 */
public final class MovementPosture {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Ширина стоя — как у ванильного игрока. */
    public static final float VANILLA_PLAYER_WIDTH = 0.6f;
    /** Высота стоя — как у ванильного игрока. */
    public static final float VANILLA_PLAYER_HEIGHT = 1.8f;

    // ============================================================
    // Вся геометрия поз захардкожена здесь (единый центр настройки).
    // Намеренно НЕ в конфиге: значения в serverconfig «липкие», а так
    // пересборка сразу применяет новые числа — удобно крутить по логам.
    //
    // Ограничение Minecraft: хитбокс игрока — ВСЕГДА центрированный квадрат
    // (ширина×ширина)×высота, привязанный к позиции (ногам). Сделать длинный/
    // повёрнутый бокс под лежащее тело нельзя. Поэтому лёжа модель центрируем
    // на якоре, а бокс берём более широким квадратом, чтобы он максимально
    // накрывал тело (кончики головы/ног всё равно немного торчат).
    // ============================================================

    // НЕ final: значения крутятся в игре через MovementTuning (movement.debug),
    // чтобы подобрать геометрию вживую, а финальные числа потом зашить как дефолты.

    // --- Хитбокс СИДЯ --- (значения подобраны в игре)
    // Бокс всегда стоит на ногах (на земле); модель опущена на SIT_MODEL_Y_OFFSET.
    public static float SIT_WIDTH = 0.6f;
    public static float SIT_HEIGHT = 1.85f;
    public static float SIT_EYE_HEIGHT = 1.1f;

    // --- Хитбокс ЛЁЖА (прямоугольник вдоль тела) ---
    // Реальный бокс лёжа строится вручную в CrawlBoundingBoxMixin: длинный вдоль тела
    // (CRAWL_LENGTH), узкий поперёк (CRAWL_WIDTH), высота CRAWL_HEIGHT. EntityDimensions
    // умеет только квадрат, поэтому осево-выровненный AABB пересчитывается из yaw тела —
    // длинный по направлению, куда лежишь, и узкий в стороны (в тоннеле не раздувает).
    public static float CRAWL_WIDTH = 0.6f;   // поперёк тела
    public static float CRAWL_LENGTH = 2.0f;  // вдоль тела (голова→ноги)
    public static float CRAWL_HEIGHT = 0.6f;
    public static float CRAWL_EYE_HEIGHT = 0.3f;
    /** Сдвиг ЦЕНТРА бокса вперёд (к голове) вдоль тела. Видимая модель вынесена вперёд
     *  (CRAWL_MODEL_FORWARD), а бокс иначе стоит на якоре-ногах и накрывает только «низ»
     *  тела. Этим сдвигаем бокс на видимое тело. Направление берём как у выноса глаз. */
    public static float CRAWL_BOX_FORWARD = 0.9f;

    // --- Докрутка точки обзора (камера 1-го лица + глаза/стрельба) ---
    /** Вертикальная докрутка сидя (блоки, + = вверх). */
    public static float SIT_Y_OFFSET = 0.0f;
    /** Вертикальная докрутка лёжа (блоки, + = вверх). */
    public static float CRAWL_Y_OFFSET = 0.0f;
    /** Вынос камеры/глаз вдоль направления тела лёжа — к голове прон-модели (≈ полдлины тела). */
    public static float CRAWL_CAMERA_FORWARD = -0.9f;

    // --- Рендер модели ---
    /** Опускание модели сидя при рендере (ТОЛЬКО 3-е лицо; на хитбокс/камеру/глаза не влияет).
     *  Сидя ноги поджаты и стопы поднимаются до ~0.63 над землёй, поэтому при 0.0 модель «висит».
     *  Можно опустить примерно до -0.6, чтобы таз сел на землю и стопы при этом не ушли под блок.
     *  Глубже ~-0.63 стопы/таз начинают проваливаться в землю. Точное число подбирать в игре
     *  ([ / ] по sit.modelYOffset при movement.debug), затем зашить сюда. */
    public static float SIT_MODEL_Y_OFFSET = -0.55f;
    /** Сдвиг прон-модели вдоль её оси: центрирует тело серединой на якоре (≈ полдлины тела ~1.0).
     *  Хитбокс ВСЕГДА центрирован на якоре, поэтому модель тоже надо центрировать сюда же,
     *  иначе передок (голова) вылезает за бокс. */
    public static float CRAWL_MODEL_FORWARD = -1.0f;
    /** Боковой/вертикальный доводчик прон-модели при рендере. */
    public static float CRAWL_MODEL_Z = 0.1f;

    private MovementPosture() {
    }

    /**
     * Горизонтальный центр бокса лёжа со сдвигом вперёд к голове ({@link #CRAWL_BOX_FORWARD},
     * направление как у выноса глаз). Единый источник для осевого AABB ({@code CrawlBoundingBoxMixin})
     * и для OBB-теста попаданий пуль ({@code CrawlObb}), чтобы они не разъезжались. Возвращает {x, z}.
     */
    public static double[] crawlCenterXZ(double posX, double posZ, float yawDeg) {
        float yRad = yawDeg * Mth.DEG_TO_RAD;
        double exo = -Mth.sin(yRad) * CRAWL_CAMERA_FORWARD;
        double ezo = Mth.cos(yRad) * CRAWL_CAMERA_FORWARD;
        double elen = Math.sqrt(exo * exo + ezo * ezo);
        double sx = elen > 1e-6 ? exo / elen * CRAWL_BOX_FORWARD : 0.0;
        double sz = elen > 1e-6 ? ezo / elen * CRAWL_BOX_FORWARD : 0.0;
        return new double[]{ posX + sx, posZ + sz };
    }

    public static boolean canFitStanding(Player player) {
        return canFitAabb(player, VANILLA_PLAYER_WIDTH, VANILLA_PLAYER_HEIGHT);
    }

    public static boolean canFitSitting(Player player) {
        return canFitAabb(player, SIT_WIDTH, SIT_HEIGHT);
    }

    /**
     * Same narrow pre-check as ModularMovements {@code ClientLitener.onSit}:
     * AABB from (x-0.1, y+0.1, z-0.1) to (x+0.1, y+1.2, z+0.1).
     * Seated hitbox after entering remains {@code sitWidth} x {@code sitHeight} from config.
     */
    public static boolean canEnterSitLikeModularMovements(Player player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        AABB box = new AABB(x - 0.1, y + 0.1, z - 0.1, x + 0.1, y + 1.2, z + 0.1);
        return player.level().noCollision(player, box);
    }

    public static boolean canFitCrawling(Player player) {
        // Use vanilla footprint for entry probe; the wide prone hitbox is set after entering.
        float probeWidth = Math.min(CRAWL_WIDTH, VANILLA_PLAYER_WIDTH);
        return canFitAabb(player, probeWidth, CRAWL_HEIGHT);
    }

    /**
     * AABB от ног игрока вверх (как в клиентском canFitAt).
     */
    public static boolean canFitAabb(Player player, float width, float height) {
        AABB box = new AABB(
            player.getX() - width / 2, player.getY(), player.getZ() - width / 2,
            player.getX() + width / 2, player.getY() + height, player.getZ() + width / 2
        );
        return player.level().noCollision(player, box);
    }

    /**
     * Направление горизонтального движения или взгляда, если за тик смещения почти не было.
     */
    public static Vec3 horizontalMotionOrLook(Player player) {
        Vec3 motion = new Vec3(player.getX() - player.xOld, 0, player.getZ() - player.zOld);
        if (motion.lengthSqr() > 1e-6) {
            return motion.normalize();
        }
        float yRad = player.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yRad), 0, Mth.cos(yRad));
    }

    /**
     * Единый источник смещения точки обзора для позы: применяется и к камере (1-е лицо в
     * {@code CameraOffsetMixin}), и к позиции глаз/точке стрельбы ({@code PlayerEyePositionMixin}),
     * чтобы они не разъезжались друг с другом и с видимой моделью. «Сидя» — только вертикаль;
     * «ползком» — дополнительно вынос вперёд вдоль направления тела к выставленной вперёд голове
     * прон-модели (хитбокс остаётся в центре). Наклон влево/вправо обрабатывается отдельно в самих
     * миксинах. {@code partialTick} сглаживает поворот тела между тиками при рендере.
     */
    public static Vec3 cameraEyeOffset(Player player, PlayerState state, float partialTick) {
        if (state == null) {
            return Vec3.ZERO;
        }
        if (state.isCrawling()) {
            float bodyYaw = Mth.rotLerp(partialTick, player.yBodyRotO, player.yBodyRot);
            float yRad = bodyYaw * Mth.DEG_TO_RAD;
            double forward = CRAWL_CAMERA_FORWARD;
            return new Vec3(-Mth.sin(yRad) * forward, CRAWL_Y_OFFSET, Mth.cos(yRad) * forward);
        }
        if (state.isSitting()) {
            return new Vec3(0, SIT_Y_OFFSET, 0);
        }
        return Vec3.ZERO;
    }

    /**
     * Дебаг-лог фактического хитбокса игрока «в мире» и точки обзора. Включается флагом
     * {@code movement.debug}. Печатает позицию, габариты, реальный bounding box (min/max по осям),
     * высоту/позицию глаз и активные оффсеты позы — чтобы по логам понять, как бокс стоит
     * относительно модели. {@code side} — "CLIENT" или "SERVER".
     */
    public static void logHitbox(String side, Player player, PlayerState state) {
        if (!Config.Movement.debug) {
            return;
        }
        String pose = state == null ? "null"
            : state.isCrawling() ? "crawl" : state.isSitting() ? "sit" : "stand";
        AABB box = player.getBoundingBox();
        Vec3 eye = player.getEyePosition();
        LOGGER.info(String.format(
            "[MovementDebug/%s] pose=%s pos=(%.3f, %.3f, %.3f) bbW=%.3f bbH=%.3f eyeH=%.3f "
                + "box=[x %.3f..%.3f | y %.3f..%.3f | z %.3f..%.3f] eyePos=(%.3f, %.3f, %.3f) "
                + "yBodyRot=%.1f | offsets: sitY=%.2f crawlY=%.2f crawlFwd=%.2f sitModelY=%.2f",
            side, pose,
            player.getX(), player.getY(), player.getZ(),
            player.getBbWidth(), player.getBbHeight(), player.getEyeHeight(),
            box.minX, box.maxX, box.minY, box.maxY, box.minZ, box.maxZ,
            eye.x, eye.y, eye.z, player.yBodyRot,
            SIT_Y_OFFSET, CRAWL_Y_OFFSET, CRAWL_CAMERA_FORWARD, SIT_MODEL_Y_OFFSET));
    }

    /**
     * Проверка перехода между закодированными состояниями (сидя/ползком/стоя + код наклона в младших разрядах).
     */
    public static boolean canApplyMovementCode(Player player, int oldCode, int newCode) {
        if (oldCode == newCode) {
            return true;
        }
        PlayerState from = new PlayerState();
        from.readCode(oldCode);
        PlayerState to = new PlayerState();
        to.readCode(newCode);

        boolean fromStance = from.isSitting() || from.isCrawling();

        if (fromStance && !to.isSitting() && !to.isCrawling()) {
            return canFitStanding(player);
        }
        if (!from.isSitting() && to.isSitting() && !to.isCrawling()) {
            return player.onGround() && canEnterSitLikeModularMovements(player);
        }
        if (!from.isCrawling() && to.isCrawling()) {
            return player.onGround() && canFitCrawling(player);
        }
        if (from.isCrawling() && to.isSitting()) {
            return player.onGround() && canEnterSitLikeModularMovements(player);
        }
        if (from.isSitting() && to.isCrawling()) {
            return player.onGround() && canFitCrawling(player);
        }
        return true;
    }
}
