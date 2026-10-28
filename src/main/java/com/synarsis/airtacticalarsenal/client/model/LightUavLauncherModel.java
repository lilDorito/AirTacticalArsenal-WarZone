package com.synarsis.airtacticalarsenal.client.model;

import com.synarsis.airtacticalarsenal.ShahedMod;
import com.synarsis.airtacticalarsenal.entity.LightUavLauncherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class LightUavLauncherModel extends GeoModel<LightUavLauncherEntity> {

    private static final ResourceLocation MODEL_EMPTY = new ResourceLocation(ShahedMod.MOD_ID, "geo/light_uav_launcher.geo.json");
    private static final ResourceLocation MODEL_LOADED = new ResourceLocation(ShahedMod.MOD_ID, "geo/light_uav_launcher_loaded.geo.json");

    private static final ResourceLocation TEXTURE_EMPTY = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/light_uav_launcher.png");
    private static final ResourceLocation TEXTURE_LOADED = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/light_uav_launcher_loaded.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ShahedMod.MOD_ID, "animations/orlan.animation.json");

    @Override
    public ResourceLocation getModelResource(LightUavLauncherEntity animatable) {
        return animatable.hasDrone() && animatable.getDroneKind().isEmpty() ? MODEL_LOADED : MODEL_EMPTY;
    }

    @Override
    public ResourceLocation getTextureResource(LightUavLauncherEntity animatable) {
        return animatable.hasDrone() && animatable.getDroneKind().isEmpty() ? TEXTURE_LOADED : TEXTURE_EMPTY;
    }

    @Override
    public ResourceLocation getAnimationResource(LightUavLauncherEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(LightUavLauncherEntity launcher, long instanceId,
                                    software.bernie.geckolib.core.animation.AnimationState<LightUavLauncherEntity> state) {
        super.setCustomAnimations(launcher, instanceId, state);
        software.bernie.geckolib.core.animatable.model.CoreGeoBone propeller = getAnimationProcessor().getBone("proppeler");
        if (propeller != null) {
            float partial = state == null ? 0.0f : state.getPartialTick();
            propeller.setRotZ(com.synarsis.airtacticalarsenal.client.LauncherSpin.angle(launcher, partial, ORLAN_PROP_RAD_PER_TICK));
        }
    }

    private static final float ORLAN_PROP_RAD_PER_TICK = (float) Math.toRadians(1260.0) / 20.0f;
}
