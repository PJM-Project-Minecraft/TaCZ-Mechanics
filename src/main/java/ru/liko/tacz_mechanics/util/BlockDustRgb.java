package ru.liko.tacz_mechanics.util;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Серверное приближение цвета пыли от блока (по карте цветов), без кастомных {@code ParticleOptions}.
 */
public final class BlockDustRgb {

    private BlockDustRgb() {}

    public static float[] rgb(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return new float[]{0.78f, 0.78f, 0.79f};
        }
        try {
            MapColor mapColor = state.getMapColor(level, pos);
            if (mapColor != null && mapColor != MapColor.NONE) {
                int c = mapColor.col;
                float r = ((c >> 16) & 255) / 255.0f;
                float g = ((c >> 8) & 255) / 255.0f;
                float b = (c & 255) / 255.0f;
                return new float[]{
                    Mth.lerp(0.5f, r, 0.9f),
                    Mth.lerp(0.5f, g, 0.9f),
                    Mth.lerp(0.5f, b, 0.9f)
                };
            }
        } catch (Exception ignored) {
        }
        if (state.is(Blocks.GRASS_BLOCK)) {
            return new float[]{0.35f, 0.72f, 0.38f};
        }
        return new float[]{0.78f, 0.78f, 0.79f};
    }

    /** Более «мягкая» вторичная дымка-корона над основным RGB */
    public static float[] puffRgb(float[] base) {
        return new float[]{
            Mth.clamp(Mth.lerp(base[0], 1.0f, 0.28f), 0.0f, 1.0f),
            Mth.clamp(Mth.lerp(base[1], 1.0f, 0.28f), 0.0f, 1.0f),
            Mth.clamp(Mth.lerp(base[2], 1.0f, 0.28f), 0.0f, 1.0f)
        };
    }
}
