package com.synarsis.airtacticalarsenal.particle;

import com.synarsis.airtacticalarsenal.ShahedMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLES = 
        DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, ShahedMod.MOD_ID);

    public static final RegistryObject<SimpleParticleType> DEBRIS = 
        PARTICLES.register("debris", () -> new SimpleParticleType(false));

    public static final RegistryObject<SimpleParticleType> JET_FLAME =
        PARTICLES.register("jet_flame", () -> new SimpleParticleType(true));

    public static final RegistryObject<SimpleParticleType> JET_FLAME_LARGE =
        PARTICLES.register("jet_flame_large", () -> new SimpleParticleType(true));
}
