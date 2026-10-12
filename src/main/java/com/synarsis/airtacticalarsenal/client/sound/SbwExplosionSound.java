package com.synarsis.airtacticalarsenal.client.sound;

import com.synarsis.airtacticalarsenal.compat.SuperbWarfareCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SbwExplosionSound {

    private static final String[] LAYERS = {"huge_explosion_close", "huge_explosion_far", "huge_explosion_very_far"};
    private static final String[] EPIC_LAYERS = {"epic_explosion_close", "epic_explosion_far", "epic_explosion_very_far"};
    public static final float[] SHAHED = {96f, 320f, 1024f};
    public static final float[] ISKANDER = {192f, 512f, 2048f};
    private static final double FALLOFF = 0.45;

    private SbwExplosionSound() {}

    public static boolean play(Vec3 pos, float[] reaches, float pitch) {
        return play(pos, reaches, pitch, LAYERS);
    }

    public static boolean playEpic(Vec3 pos, float[] reaches, float pitch) {
        boolean epic = true;
        for (String layer : EPIC_LAYERS) {
            epic &= net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS
                    .containsKey(new ResourceLocation("superbwarfare", layer));
        }
        return play(pos, reaches, pitch, epic ? EPIC_LAYERS : LAYERS);
    }

    private static boolean play(Vec3 pos, float[] reaches, float pitch, String[] layers) {
        if (!available()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return true;
        Vec3 listener = mc.cameraEntity != null ? mc.cameraEntity.position() : mc.player.position();
        double distance = listener.distanceTo(pos);
        float[] gains = gains(distance, reaches);
        long delayMs = (long) (distance / 17.0) * 50L;
        Runnable play = () -> {
            for (int i = 0; i < layers.length; i++) {
                if (gains[i] < 0.01f) continue;
                mc.getSoundManager().play(new SimpleSoundInstance(
                        new ResourceLocation("superbwarfare", layers[i]), SoundSource.BLOCKS, gains[i], pitch,
                        SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE,
                        pos.x, pos.y, pos.z, false));
            }
        };
        if (delayMs <= 0) {
            play.run();
        } else {
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException ignored) {
                    return;
                }
                mc.execute(play);
            }, "ATA explosion sound");
            t.setDaemon(true);
            t.start();
        }
        return true;
    }

    public static void playAta(Vec3 pos, double reach, float pitch) {
        playAta(pos, reach, 0.5, pitch);
    }

    public static void playAta(Vec3 pos, double reach, double falloff, float pitch) {
        net.minecraft.sounds.SoundEvent[] pool = {
                com.synarsis.airtacticalarsenal.sound.ModSounds.EXPLOSION_1.get(),
                com.synarsis.airtacticalarsenal.sound.ModSounds.EXPLOSION_2.get(),
                com.synarsis.airtacticalarsenal.sound.ModSounds.EXPLOSION_3.get(),
                com.synarsis.airtacticalarsenal.sound.ModSounds.EXPLOSION_4.get(),
                com.synarsis.airtacticalarsenal.sound.ModSounds.EXPLOSION_5.get()};
        playSingle(pos, pool[(int) (Math.random() * pool.length)].getLocation(), reach, falloff, 1.0f, pitch);
    }

    public static void playAirBurst(Vec3 pos) {
        ResourceLocation air = new ResourceLocation("superbwarfare", "explosion_air");
        if (SuperbWarfareCompat.isLoaded() && net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS.containsKey(air)) {
            playSingle(pos, air, 1200.0, FALLOFF, 1.0f, 0.9f + (float) Math.random() * 0.2f);
        } else {
            playAta(pos, 700.0, 1.25f + (float) Math.random() * 0.1f);
        }
    }

    private static void playSingle(Vec3 pos, ResourceLocation sound, double reach, double falloff, float maxGain, float pitch) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Vec3 listener = mc.cameraEntity != null ? mc.cameraEntity.position() : mc.player.position();
        double distance = listener.distanceTo(pos);
        if (distance >= reach) return;
        float gain = (float) (maxGain * Math.pow(1.0 - distance / reach, falloff));
        if (gain < 0.01f) return;
        long delayMs = (long) (distance / 17.0) * 50L;
        Runnable play = () -> mc.getSoundManager().play(new SimpleSoundInstance(sound, SoundSource.BLOCKS, gain, pitch,
                SoundInstance.createUnseededRandom(), false, 0, SoundInstance.Attenuation.NONE, pos.x, pos.y, pos.z, false));
        if (delayMs <= 0) {
            play.run();
            return;
        }
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                return;
            }
            mc.execute(play);
        }, "ATA explosion sound");
        t.setDaemon(true);
        t.start();
    }

    private static Boolean available;

    private static boolean available() {
        if (available == null) {
            boolean ok = SuperbWarfareCompat.isLoaded();
            for (int i = 0; ok && i < LAYERS.length; i++) {
                ok = net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS
                        .containsKey(new ResourceLocation("superbwarfare", LAYERS[i]));
            }
            available = ok;
        }
        return available;
    }

    static float[] gains(double distance, float[] ranges) {
        int n = ranges.length;
        float[] out = new float[n];
        double reach = ranges[n - 1];
        if (distance >= reach) return out;
        double loudness = Math.pow(1 - distance / reach, FALLOFF);
        double reached = 1.0;
        for (int k = 0; k < n; k++) {
            double blend = 1.0;
            if (k < n - 1) {
                double edge = 0.75 * ranges[k];
                double lo = edge * 0.75, hi = edge * 1.25;
                blend = Math.max(0.0, Math.min(1.0, (distance - lo) / (hi - lo)));
            }
            double stay = k == n - 1 ? 1.0 : 1.0 - blend;
            out[k] = (float) (loudness * Math.sqrt(reached * stay));
            if (k < n - 1) reached *= blend;
        }
        return out;
    }
}
