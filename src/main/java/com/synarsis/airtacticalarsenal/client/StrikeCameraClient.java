package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.StrikeRetargetPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class StrikeCameraClient {
    private static final int NEED_LOCK_TICKS = 40;

    private static KeyMapping sbwDiveKey;
    private static boolean sbwLookedUp;
    private static boolean wasDown;
    private static Vec3 lastSent;
    private static int needLockTicks;

    private StrikeCameraClient() {
    }

    public static ShahedEntity viewed() {
        if (!OrlanCameraHandler.isCameraActive()) {
            return null;
        }
        return OrlanCameraHandler.getLinkedDrone() instanceof ShahedEntity strike && strike.isAlive() ? strike : null;
    }

    public static Vec3 lastSent() {
        return lastSent;
    }

    public static void markSent(Vec3 point) {
        lastSent = point;
        needLockTicks = 0;
    }

    public static boolean showNeedLock() {
        return needLockTicks > 0;
    }

    public static KeyMapping diveKey() {
        if (!sbwLookedUp) {
            sbwLookedUp = true;
            try {
                Class<?> keys = Class.forName("com.atsuishio.superbwarfare.init.ModKeyMappings");
                Object key = keys.getField("VEHICLE_SEEK").get(null);
                if (key instanceof KeyMapping mapping) {
                    sbwDiveKey = mapping;
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                sbwDiveKey = null;
            }
        }
        return sbwDiveKey != null ? sbwDiveKey : AtaKeys.STRIKE_LOCK;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        ShahedEntity drone = viewed();
        if (drone == null) {
            wasDown = false;
            lastSent = null;
            needLockTicks = 0;
            return;
        }
        if (needLockTicks > 0) {
            needLockTicks--;
        }
        boolean down = mc.screen == null && diveKey().isDown();
        boolean pressed = down && !wasDown;
        wasDown = down;
        if (!pressed || drone.isSteerable() || StrikeAimClient.selectMode()) {
            return;
        }
        Vec3 target = OrlanCameraHandler.isLocked() ? OrlanCameraHandler.getLockTargetPos() : null;
        if (target == null) {
            needLockTicks = NEED_LOCK_TICKS;
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.8F, 0.5F));
            return;
        }
        NetworkHandler.sendToServer(new StrikeRetargetPacket(drone.getId(), BlockPos.containing(target)));
        lastSent = target;
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING.value(), 2.0F, 0.6F));
    }

    public static Component diveHint() {
        return Component.translatable("hud.ata.strike.dive_hint", diveKey().getTranslatedKeyMessage());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!OrlanCameraHandler.isCameraActive()) {
            return;
        }
        event.setSwingHand(false);
        event.setCanceled(true);
    }
}
