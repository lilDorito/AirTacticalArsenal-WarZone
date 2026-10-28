package com.synarsis.airtacticalarsenal.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class JetFlameParticle extends TextureSheetParticle {
    public static float nextScale = 1.0f;

    private final SpriteSet sprites;
    private final float initialScale;

    protected JetFlameParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz,
                               SpriteSet sprites, float size) {
        super(level, x, y, z, dx, dy, dz);
        this.sprites = sprites;
        this.lifetime = 4 + this.random.nextInt(4);
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.xd = dx;
        this.yd = dy;
        this.zd = dz;
        this.alpha = 0.95f;
        this.initialScale = size * nextScale * (0.85f + this.random.nextFloat() * 0.3f);
        this.quadSize = this.initialScale;
        this.rCol = 1.0f;
        this.gCol = 0.75f + this.random.nextFloat() * 0.2f;
        this.bCol = 0.35f + this.random.nextFloat() * 0.2f;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        float age = (float) this.age / (float) this.lifetime;
        this.quadSize = this.initialScale * (1.0f - age * 0.6f);
        this.alpha = 0.95f * (1.0f - age);
        this.gCol = (0.75f + this.random.nextFloat() * 0.15f) * (1.0f - age * 0.5f);
        this.bCol = (0.35f + this.random.nextFloat() * 0.15f) * (1.0f - age * 0.7f);
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    public int getLightColor(float partialTick) {
        return 15 << 20 | 15 << 4;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final float size;

        public Provider(SpriteSet sprites, float size) {
            this.sprites = sprites;
            this.size = size;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double dx, double dy, double dz) {
            return new JetFlameParticle(level, x, y, z, dx, dy, dz, this.sprites, this.size);
        }
    }
}
