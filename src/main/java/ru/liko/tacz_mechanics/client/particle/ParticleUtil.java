package ru.liko.tacz_mechanics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ParticleUtil {

    private ParticleUtil() {}

    public static double[] escapeBlock(ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
        BlockPos pos = BlockPos.containing(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || !isSolidOccluding(level, pos, state)) {
            return new double[] { x, y, z };
        }
        Vec3 dir = new Vec3(xd, yd, zd);
        if (dir.lengthSqr() < 1.0e-8) {
            dir = new Vec3(0.0, 1.0, 0.0);
        } else {
            dir = dir.normalize();
        }
        double cx = x;
        double cy = y;
        double cz = z;
        for (int i = 0; i < 64; i++) {
            cx += dir.x * 0.0625;
            cy += dir.y * 0.0625;
            cz += dir.z * 0.0625;
            BlockPos p = BlockPos.containing(cx, cy, cz);
            BlockState s = level.getBlockState(p);
            if (s.isAir() || !isSolidOccluding(level, p, s)) {
                return new double[] { cx, cy, cz };
            }
        }
        return new double[] { x + dir.x * 0.1, y + dir.y * 0.1, z + dir.z * 0.1 };
    }

    private static boolean isSolidOccluding(ClientLevel level, BlockPos pos, BlockState state) {
        return state.isSolidRender(level, pos);
    }
}
