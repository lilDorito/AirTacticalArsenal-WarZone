package com.synarsis.airtacticalarsenal.client.renderer;

import com.synarsis.airtacticalarsenal.client.model.LightUavLauncherModel;
import com.synarsis.airtacticalarsenal.entity.LightUavLauncherEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class LightUavLauncherRenderer extends GeoEntityRenderer<LightUavLauncherEntity> {

    private static final float LAUNCHER_SCALE = 1.0F;

    public LightUavLauncherRenderer(EntityRendererProvider.Context context) {
        super(context, new LightUavLauncherModel());
        this.shadowRadius = 0.6F;
    }

    @Override
    public void render(LightUavLauncherEntity entity, float entityYaw, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - entityYaw));
        poseStack.scale(LAUNCHER_SCALE, LAUNCHER_SCALE, LAUNCHER_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        if (entity.hasDrone()) {
            com.synarsis.airtacticalarsenal.api.DroneVariantsClient.LauncherDisplay display = com.synarsis.airtacticalarsenal.api.DroneVariantsClient.display(entity.getDroneKind());
            if (display != null) {
                display.render(entity, partialTick, poseStack, bufferSource, packedLight);
            }
        }
        poseStack.popPose();
    }
}
