package com.synarsis.airtacticalarsenal.client.model.item;

import com.synarsis.airtacticalarsenal.ShahedMod;
import com.synarsis.airtacticalarsenal.item.LightUavLauncherItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.model.GeoModel;

@OnlyIn(Dist.CLIENT)
public class LightUavLauncherItemModel extends GeoModel<LightUavLauncherItem> {

    private static final ResourceLocation MODEL = new ResourceLocation(ShahedMod.MOD_ID, "geo/light_uav_launcher.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/light_uav_launcher.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ShahedMod.MOD_ID, "animations/orlan.animation.json");

    @Override
    public ResourceLocation getModelResource(LightUavLauncherItem item) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(LightUavLauncherItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(LightUavLauncherItem item) {
        return ANIMATION;
    }
}
