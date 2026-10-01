package com.synarsis.airtacticalarsenal.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.synarsis.airtacticalarsenal.block.ShahedRadarBlock;
import com.synarsis.airtacticalarsenal.block.entity.ShahedRadarBlockEntity;
import com.synarsis.airtacticalarsenal.client.SbwRadarLook;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShahedRadarRenderer implements BlockEntityRenderer<ShahedRadarBlockEntity> {

    private final GeoPart geo = new GeoPart();

    public ShahedRadarRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ShahedRadarBlockEntity animatable, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!ShahedRadarBlock.fumoModel || SbwRadarLook.model() == null) return;
        PoseStack.Pose top = poseStack.last();
        try {
            geo.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        } catch (Throwable t) {
            while (poseStack.last() != top && !poseStack.clear()) poseStack.popPose();
            SbwRadarLook.disable("render failed", t);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ShahedRadarBlockEntity blockEntity) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    private static class GeoPart extends GeoBlockRenderer<ShahedRadarBlockEntity> {
        GeoPart() {
            super(new FuMO25LookModel());
        }

        @Override
        public RenderType getRenderType(ShahedRadarBlockEntity animatable, ResourceLocation texture,
                                        @Nullable MultiBufferSource bufferSource, float partialTick) {
            return RenderType.entityTranslucent(texture);
        }
    }

    public static class FuMO25LookModel extends GeoModel<ShahedRadarBlockEntity> {
        private static final ResourceLocation MODEL = new ResourceLocation("ata", "geo/fumo_25_look.geo.json");
        private static final ResourceLocation FALLBACK_TEXTURE = new ResourceLocation("superbwarfare", "textures/block/fumo_25.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation("ata", "animations/fumo_25_look.animation.json");

        private static final int TICKS_PER_TURN = 200;

        private BakedGeoModel active;

        @Override
        public BakedGeoModel getBakedModel(ResourceLocation location) {
            BakedGeoModel model = SbwRadarLook.model();
            if (model == null) return super.getBakedModel(location);
            if (model != active) {
                getAnimationProcessor().setActiveModel(model);
                active = model;
            }
            return model;
        }

        @Override
        public ResourceLocation getModelResource(ShahedRadarBlockEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(ShahedRadarBlockEntity animatable) {
            ResourceLocation texture = SbwRadarLook.texture();
            return texture != null ? texture : FALLBACK_TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(ShahedRadarBlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(ShahedRadarBlockEntity animatable, long instanceId,
                                        AnimationState<ShahedRadarBlockEntity> animationState) {
            String name = SbwRadarLook.spinBone();
            if (name == null) return;
            CoreGeoBone bone = getAnimationProcessor().getBone(name);
            if (bone == null) return;
            Level level = animatable.getLevel();
            if (level == null) return;
            long seed = animatable.getBlockPos().asLong();
            int phase = (int) Math.floorMod(seed ^ (seed >>> 21), (long) TICKS_PER_TURN);
            float t = (level.getGameTime() + phase) % TICKS_PER_TURN + animationState.getPartialTick();
            bone.setRotY(t / TICKS_PER_TURN * ((float) Math.PI * 2.0f));
        }
    }
}
