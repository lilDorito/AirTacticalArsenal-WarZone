package com.synarsis.airtacticalarsenal.client.renderer;

import com.synarsis.airtacticalarsenal.block.entity.ShahedRadarBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import javax.annotation.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShahedRadarRenderer extends GeoBlockRenderer<ShahedRadarBlockEntity> {

    public ShahedRadarRenderer(BlockEntityRendererProvider.Context context) {
        super(new FuMO25LookModel());
    }

    @Override
    public RenderType getRenderType(ShahedRadarBlockEntity animatable, ResourceLocation texture,
                                    @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public boolean shouldRenderOffScreen(ShahedRadarBlockEntity blockEntity) {
        return false;
    }

    @Override
    public int getViewDistance() {
        return 512;
    }

    public static class FuMO25LookModel extends GeoModel<ShahedRadarBlockEntity> {
        private static final ResourceLocation MODEL = new ResourceLocation("superbwarfare", "geo/fumo_25.geo.json");
        private static final ResourceLocation TEXTURE = new ResourceLocation("superbwarfare", "textures/block/fumo_25.png");
        private static final ResourceLocation ANIMATION = new ResourceLocation("superbwarfare", "animations/fumo_25.animation.json");

        private static final int TICKS_PER_TURN = 200;

        @Override
        public ResourceLocation getModelResource(ShahedRadarBlockEntity animatable) {
            return MODEL;
        }

        @Override
        public ResourceLocation getTextureResource(ShahedRadarBlockEntity animatable) {
            return TEXTURE;
        }

        @Override
        public ResourceLocation getAnimationResource(ShahedRadarBlockEntity animatable) {
            return ANIMATION;
        }

        @Override
        public void setCustomAnimations(ShahedRadarBlockEntity animatable, long instanceId,
                                        AnimationState<ShahedRadarBlockEntity> animationState) {
            CoreGeoBone bone = getAnimationProcessor().getBone("mian");
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
