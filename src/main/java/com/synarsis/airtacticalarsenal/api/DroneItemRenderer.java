package com.synarsis.airtacticalarsenal.api;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class DroneItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final double SHAHED_LENGTH = 4.64;

    private final Supplier<? extends EntityType<?>> type;
    private final DoubleSupplier modelSize;
    private float scale = -1.0F;
    private Entity dummy;
    private Level dummyLevel;

    public DroneItemRenderer(Supplier<? extends EntityType<?>> type, DoubleSupplier modelSize) {
        super(dispatcher(), models());
        this.type = type;
        this.modelSize = modelSize;
    }

    private static BlockEntityRenderDispatcher dispatcher() {
        return Minecraft.getInstance().getBlockEntityRenderDispatcher();
    }

    private static EntityModelSet models() {
        return Minecraft.getInstance().getEntityModels();
    }

    public static void register(Consumer<IClientItemExtensions> consumer, Supplier<? extends EntityType<?>> type, DoubleSupplier modelSize) {
        consumer.accept(new IClientItemExtensions() {
            private DroneItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new DroneItemRenderer(type, modelSize);
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers,
                             int light, int overlay) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        if (this.scale < 0.0F) {
            this.scale = (float) Math.sqrt(SHAHED_LENGTH / Math.max(0.2, this.modelSize.getAsDouble()));
        }
        if (this.dummy == null || this.dummyLevel != mc.level) {
            this.dummy = this.type.get().create(mc.level);
            this.dummyLevel = mc.level;
        }
        Entity entity = this.dummy;
        if (entity == null) {
            return;
        }
        entity.setYRot(0.0F);
        entity.yRotO = 0.0F;
        entity.setXRot(0.0F);
        entity.xRotO = 0.0F;
        EntityRenderer<? super Entity> renderer = mc.getEntityRenderDispatcher().getRenderer(entity);
        pose.pushPose();
        pose.translate(0.5F, 0.4F, 0.5F);
        pose.scale(this.scale, this.scale, this.scale);
        renderer.render(entity, 0.0F, mc.getFrameTime(), pose, buffers, light);
        pose.popPose();
    }
}
