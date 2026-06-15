package ru.liko.tacz_mechanics.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ImpactFlashParticle extends TextureSheetParticle {

    /** 10 ticks = 0.5 s at 20 TPS; particle is removed immediately when age reaches this. */
    public static final int FLASH_LIFETIME_TICKS = 10;

    private final SpriteSet sprites;

    private ImpactFlashParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd, SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        setParticleSpeed(xd * 0.35, yd * 0.35, zd * 0.35);
        lifetime = FLASH_LIFETIME_TICKS;
        hasPhysics = false;
        friction = 1.0f;
        quadSize *= 6.0f + random.nextFloat() * 2.5f;
        setColor(1.0f, 1.0f, 1.0f);
        setAlpha(1.0f);
        setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        setSpriteFromAge(sprites);
        super.tick();
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static ParticleProvider<SimpleParticleType> create(SpriteSet sprites) {
        return (type, level, x, y, z, xd, yd, zd) -> new ImpactFlashParticle(level, x, y, z, xd, yd, zd, sprites);
    }
}
