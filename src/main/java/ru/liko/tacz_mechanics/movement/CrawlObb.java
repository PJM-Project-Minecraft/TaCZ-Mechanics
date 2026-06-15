package ru.liko.tacz_mechanics.movement;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Тест пересечения отрезка (луча пули) с ПОВЁРНУТЫМ боксом тела лёжа (OBB).
 *
 * <p>Хитбокс игрока в движке — всегда осевой AABB, поэтому лёжа он накрывает повёрнутое тело
 * вместе с пустыми углами. Этот тест отсекает попадания в углы: TaCZ ищет цель по AABB
 * (broadphase), а здесь проверяем, прошёл ли луч через сам тонкий повёрнутый бокс тела.</p>
 *
 * <p>Геометрия совпадает с {@code CrawlBoundingBoxMixin}: тот же центр ({@link
 * MovementPosture#crawlCenterXZ}), длинная ось тела {@code u=(-sin,cos)}, поперечная
 * {@code v=(cos,sin)}, габариты {@code CRAWL_LENGTH/CRAWL_WIDTH/CRAWL_HEIGHT}.</p>
 */
public final class CrawlObb {

    private CrawlObb() {
    }

    /** true — луч start→end задевает повёрнутое тело лёжа; false — прошёл мимо (через угол AABB). */
    public static boolean segmentHitsBody(Player player, Vec3 start, Vec3 end) {
        float yRad = player.yBodyRot * Mth.DEG_TO_RAD;
        double sin = Mth.sin(yRad);
        double cos = Mth.cos(yRad);

        double[] center = MovementPosture.crawlCenterXZ(player.getX(), player.getZ(), player.yBodyRot);
        double cx = center[0];
        double cz = center[1];
        double cy = player.getY() + MovementPosture.CRAWL_HEIGHT / 2.0;

        double halfAlong = MovementPosture.CRAWL_LENGTH / 2.0;   // вдоль тела
        double halfAcross = MovementPosture.CRAWL_WIDTH / 2.0;   // поперёк тела
        double halfUp = MovementPosture.CRAWL_HEIGHT / 2.0;      // по вертикали

        double[] s = toLocal(start, cx, cy, cz, sin, cos);
        double[] e = toLocal(end, cx, cy, cz, sin, cos);
        return segmentAabb(s, e, halfAlong, halfUp, halfAcross);
    }

    /** Переводит мировую точку в локальные координаты бокса: {вдоль, вверх, поперёк}. */
    private static double[] toLocal(Vec3 p, double cx, double cy, double cz, double sin, double cos) {
        double dx = p.x - cx;
        double dy = p.y - cy;
        double dz = p.z - cz;
        double along = dx * (-sin) + dz * cos;   // ось u=(-sin,cos)
        double across = dx * cos + dz * sin;     // ось v=(cos,sin)
        return new double[]{ along, dy, across };
    }

    /** Пересечение отрезка s→e с центрированным AABB ±(hx,hy,hz). Метод слэбов. */
    private static boolean segmentAabb(double[] s, double[] e, double hx, double hy, double hz) {
        double[] h = { hx, hy, hz };
        double tmin = 0.0;
        double tmax = 1.0;
        for (int i = 0; i < 3; i++) {
            double d = e[i] - s[i];
            if (Math.abs(d) < 1.0e-9) {
                if (s[i] < -h[i] || s[i] > h[i]) {
                    return false;
                }
            } else {
                double t1 = (-h[i] - s[i]) / d;
                double t2 = (h[i] - s[i]) / d;
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                if (t1 > tmin) tmin = t1;
                if (t2 < tmax) tmax = t2;
                if (tmin > tmax) {
                    return false;
                }
            }
        }
        return true;
    }
}
