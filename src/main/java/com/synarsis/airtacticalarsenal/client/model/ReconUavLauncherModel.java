package com.synarsis.airtacticalarsenal.client.model;

import com.synarsis.airtacticalarsenal.ShahedMod;
import com.synarsis.airtacticalarsenal.entity.ReconUavLauncherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class ReconUavLauncherModel extends GeoModel<ReconUavLauncherEntity> {

    private static final ResourceLocation MODEL_EMPTY = new ResourceLocation(ShahedMod.MOD_ID, "geo/recon_uav_launcher.geo.json");
    private static final ResourceLocation MODEL_LOADED = new ResourceLocation(ShahedMod.MOD_ID, "geo/recon_uav_launcher_loaded.geo.json");

    private static final ResourceLocation TEXTURE_EMPTY = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/recon_uav_launcher.png");
    private static final ResourceLocation TEXTURE_LOADED = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/recon_uav_launcher_loaded.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ShahedMod.MOD_ID, "animations/orlan.animation.json");

    @Override
    public ResourceLocation getModelResource(ReconUavLauncherEntity animatable) {
        return animatable.hasDrone() && animatable.getDroneKind().isEmpty() ? MODEL_LOADED : MODEL_EMPTY;
    }

    @Override
    public ResourceLocation getTextureResource(ReconUavLauncherEntity animatable) {
        return animatable.hasDrone() && animatable.getDroneKind().isEmpty() ? TEXTURE_LOADED : TEXTURE_EMPTY;
    }

    @Override
    public ResourceLocation getAnimationResource(ReconUavLauncherEntity animatable) {
        return ANIMATION;
    }
}
