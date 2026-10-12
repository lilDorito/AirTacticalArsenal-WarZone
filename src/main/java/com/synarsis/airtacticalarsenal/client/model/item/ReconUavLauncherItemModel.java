package com.synarsis.airtacticalarsenal.client.model.item;

import com.synarsis.airtacticalarsenal.ShahedMod;
import com.synarsis.airtacticalarsenal.item.ReconUavLauncherItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.model.GeoModel;

@OnlyIn(Dist.CLIENT)
public class ReconUavLauncherItemModel extends GeoModel<ReconUavLauncherItem> {

    private static final ResourceLocation MODEL = new ResourceLocation(ShahedMod.MOD_ID, "geo/recon_uav_launcher.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ShahedMod.MOD_ID, "textures/entity/recon_uav_launcher.png");
    private static final ResourceLocation ANIMATION = new ResourceLocation(ShahedMod.MOD_ID, "animations/orlan.animation.json");

    @Override
    public ResourceLocation getModelResource(ReconUavLauncherItem item) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ReconUavLauncherItem item) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ReconUavLauncherItem item) {
        return ANIMATION;
    }
}
