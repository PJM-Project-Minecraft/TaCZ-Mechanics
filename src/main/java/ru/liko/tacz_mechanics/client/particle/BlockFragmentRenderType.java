package ru.liko.tacz_mechanics.client.particle;

import net.minecraft.client.particle.ParticleRenderType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class BlockFragmentRenderType {

    public static final ParticleRenderType INSTANCE = ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;

    private BlockFragmentRenderType() {}
}
