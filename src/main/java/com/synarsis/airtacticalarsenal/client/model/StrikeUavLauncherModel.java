package com.synarsis.airtacticalarsenal.client.model;

import com.synarsis.airtacticalarsenal.ShahedMod;
import com.synarsis.airtacticalarsenal.entity.StrikeUavLauncherEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class StrikeUavLauncherModel extends GeoModel<StrikeUavLauncherEntity> {

    private static final ResourceLocation MODEL_EMPTY = new ResourceLocation(ShahedMod.MOD_ID, "geo/strike_uav_launcher.geo.json");

    private static final ResourceLocation MODEL_LOADED = new ResourceLocation(ShahedMod.MOD_ID, "geo/strike_uav_launcher_loaded.geo.json");

    private static final ResourceLocation MODEL_LOADED_BLACK = new ResourceLocation(ShahedMod.MOD_ID, "geo/strike_uav_launcher_loaded_black.geo.json");

    private static final ResourceLocation TEXTURE_EMPTY = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/strike_uav_launcher.png");

    private static final ResourceLocation TEXTURE_LOADED = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/strike_uav_launcher_loaded.png");

    private static final ResourceLocation TEXTURE_LOADED_BLACK = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/strike_uav_launcher_loaded_black.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ShahedMod.MOD_ID, "animations/shahed.animation.json");

    @Override
    public ResourceLocation getModelResource(StrikeUavLauncherEntity animatable) {
        if (!animatable.hasDrone() || !animatable.getDroneKind().isEmpty()) return MODEL_EMPTY;
        return "black".equals(animatable.getShahedColor()) ? MODEL_LOADED_BLACK : MODEL_LOADED;
    }

    @Override
    public ResourceLocation getTextureResource(StrikeUavLauncherEntity animatable) {
        if (!animatable.hasDrone() || !animatable.getDroneKind().isEmpty()) return TEXTURE_EMPTY;
        return "black".equals(animatable.getShahedColor()) ? TEXTURE_LOADED_BLACK : TEXTURE_LOADED;
    }

    @Override
    public ResourceLocation getAnimationResource(StrikeUavLauncherEntity animatable) {
        return ANIMATION;
    }
}
