package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.block.entity.ModBlockEntities;
import com.synarsis.airtacticalarsenal.client.renderer.ShahedRenderer;
import com.synarsis.airtacticalarsenal.client.renderer.HeavyUavLauncherRenderer;
import com.synarsis.airtacticalarsenal.client.renderer.IskanderRenderer;
import com.synarsis.airtacticalarsenal.client.renderer.IskanderRocketBlockRenderer;
import com.synarsis.airtacticalarsenal.client.renderer.PaintStandRenderer;
import com.synarsis.airtacticalarsenal.client.renderer.OrlanRenderer;
import com.synarsis.airtacticalarsenal.client.renderer.LightUavLauncherRenderer;
import com.synarsis.airtacticalarsenal.entity.ModEntities;
import com.synarsis.airtacticalarsenal.particle.DebrisParticle;
import com.synarsis.airtacticalarsenal.particle.ModParticles;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHAHED.get(), ShahedRenderer::new);
        event.registerEntityRenderer(ModEntities.ISKANDER.get(), IskanderRenderer::new);
        event.registerEntityRenderer(ModEntities.HEAVY_UAV_LAUNCHER.get(), HeavyUavLauncherRenderer::new);
        event.registerEntityRenderer(ModEntities.PAINT_STAND.get(), PaintStandRenderer::new);
        event.registerEntityRenderer(ModEntities.ORLAN.get(), OrlanRenderer::new);
        event.registerEntityRenderer(ModEntities.LIGHT_UAV_LAUNCHER.get(), LightUavLauncherRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.ISKANDER_ROCKET.get(), IskanderRocketBlockRenderer::new);
        if (com.synarsis.airtacticalarsenal.block.ShahedRadarBlock.SBW_LOOK) {
            event.registerBlockEntityRenderer(ModBlockEntities.SHAHED_RADAR.get(),
                    com.synarsis.airtacticalarsenal.client.renderer.ShahedRadarRenderer::new);
        }
        System.out.println("[ATA] Renderers registered for ShahedEntity, IskanderEntity, HeavyUavLauncher, LightUavLauncher, PaintStand, Orlan and IskanderRocketBlock!");
    }

    @SubscribeEvent
    public static void onClientSetup(net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            com.synarsis.airtacticalarsenal.api.DroneVariantsClient.registerFarSound(
                    new net.minecraft.resources.ResourceLocation("ata", "shahed"),
                    com.synarsis.airtacticalarsenal.sound.ModSounds.SHAHED_ENGINE_FAR, -19.7f,
                    com.synarsis.airtacticalarsenal.api.DroneVariantsClient.SoundClass.HEAVY);
            com.synarsis.airtacticalarsenal.api.DroneVariantsClient.registerFarSound(
                    new net.minecraft.resources.ResourceLocation("ata", "orlan"),
                    com.synarsis.airtacticalarsenal.sound.ModSounds.ORLAN_ENGINE_FAR, -19.9f,
                    com.synarsis.airtacticalarsenal.api.DroneVariantsClient.SoundClass.LIGHT);
        });
    }

    @SubscribeEvent
    public static void registerOverlays(net.minecraftforge.client.event.RegisterGuiOverlaysEvent event) {
        event.registerAbove(net.minecraftforge.client.gui.overlay.VanillaGuiOverlay.HOTBAR.id(), "vehicle_radar",
                com.synarsis.airtacticalarsenal.client.radar.VehicleRadarHud.OVERLAY);
    }

    @SubscribeEvent
    public static void registerKeys(net.minecraftforge.client.event.RegisterKeyMappingsEvent event) {
        event.register(AtaKeys.VEHICLE_RADAR);
        event.register(AtaKeys.STRIKE_LOCK);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.DEBRIS.get(), DebrisParticle.Provider::new);
        event.registerSpriteSet(ModParticles.JET_FLAME.get(),
                sprites -> new com.synarsis.airtacticalarsenal.particle.JetFlameParticle.Provider(sprites, 0.55f));
        event.registerSpriteSet(ModParticles.JET_FLAME_LARGE.get(),
                sprites -> new com.synarsis.airtacticalarsenal.particle.JetFlameParticle.Provider(sprites, 0.95f));
        System.out.println("[ATA] Debris particle registered!");
    }
}
