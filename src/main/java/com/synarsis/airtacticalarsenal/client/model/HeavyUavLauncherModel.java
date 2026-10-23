package com.synarsis.airtacticalarsenal.client.model;

import com.synarsis.airtacticalarsenal.ShahedMod;
import com.synarsis.airtacticalarsenal.entity.HeavyUavLauncherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class HeavyUavLauncherModel extends GeoModel<HeavyUavLauncherEntity> {

    private static final ResourceLocation MODEL_EMPTY = new ResourceLocation(ShahedMod.MOD_ID, "geo/heavy_uav_launcher.geo.json");

    private static final ResourceLocation MODEL_LOADED = new ResourceLocation(ShahedMod.MOD_ID, "geo/heavy_uav_launcher_loaded.geo.json");

    private static final ResourceLocation MODEL_LOADED_BLACK = new ResourceLocation(ShahedMod.MOD_ID, "geo/heavy_uav_launcher_loaded_black.geo.json");

    private static final ResourceLocation TEXTURE_EMPTY = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/heavy_uav_launcher.png");

    private static final ResourceLocation TEXTURE_LOADED = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/heavy_uav_launcher_loaded.png");

    private static final ResourceLocation TEXTURE_LOADED_BLACK = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/heavy_uav_launcher_loaded_black.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ShahedMod.MOD_ID, "animations/shahed.animation.json");

    @Override
    public ResourceLocation getModelResource(HeavyUavLauncherEntity animatable) {
        if (!animatable.hasDrone() || !animatable.getDroneKind().isEmpty()) return MODEL_EMPTY;
        return "black".equals(animatable.getShahedColor()) ? MODEL_LOADED_BLACK : MODEL_LOADED;
    }

    @Override
    public ResourceLocation getTextureResource(HeavyUavLauncherEntity animatable) {
        if (!animatable.hasDrone() || !animatable.getDroneKind().isEmpty()) return TEXTURE_EMPTY;
        return "black".equals(animatable.getShahedColor()) ? TEXTURE_LOADED_BLACK : TEXTURE_LOADED;
    }

    @Override
    public ResourceLocation getAnimationResource(HeavyUavLauncherEntity animatable) {
        return ANIMATION;
    }

    @Override
    public void setCustomAnimations(HeavyUavLauncherEntity launcher, long instanceId,
                                    software.bernie.geckolib.core.animation.AnimationState<HeavyUavLauncherEntity> state) {
        super.setCustomAnimations(launcher, instanceId, state);
        software.bernie.geckolib.core.animatable.model.CoreGeoBone propeller = getAnimationProcessor().getBone("bone");
        if (propeller != null) {
            float partial = state == null ? 0.0f : state.getPartialTick();
            propeller.setRotZ(-com.synarsis.airtacticalarsenal.client.LauncherSpin.angle(launcher, partial, SHAHED_PROP_RAD_PER_TICK));
        }
    }

    private static final float SHAHED_PROP_RAD_PER_TICK = (float) Math.toRadians(1980.0) / 20.0f;
}
