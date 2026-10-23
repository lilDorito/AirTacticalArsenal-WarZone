package com.synarsis.airtacticalarsenal.client.sound;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import com.synarsis.airtacticalarsenal.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class OrlanCrashSound {

    public static final float[] ORLAN = {48f, 160f, 448f};
    private static final double FALLBACK_REACH = 300.0;
    private static final RandomSource RANDOM = RandomSource.create();

    private OrlanCrashSound() {}

    public static void play(Vec3 pos) {
        com.synarsis.airtacticalarsenal.client.OrlanCameraHandler.cutFeedIfDestroyedNear(pos);
        if (!ShahedConfig.areCustomExplosionSoundsEnabled()) return;
        float pitch = 1.15f + RANDOM.nextFloat() * 0.15f;
        if (SbwExplosionSound.play(pos, ORLAN, pitch)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        Vec3 listener = mc.cameraEntity != null ? mc.cameraEntity.position() : mc.player.position();
        double distance = listener.distanceTo(pos);
        if (distance >= FALLBACK_REACH) return;
        float volume = (float) (3.0 * Math.pow(1.0 - distance / FALLBACK_REACH, 0.6));
        var sound = switch (RANDOM.nextInt(3)) {
            case 0 -> ModSounds.EXPLOSION_1.get();
            case 1 -> ModSounds.EXPLOSION_2.get();
            default -> ModSounds.EXPLOSION_3.get();
        };
        mc.level.playLocalSound(pos.x, pos.y, pos.z, sound, SoundSource.HOSTILE, volume, pitch, false);
    }
}
