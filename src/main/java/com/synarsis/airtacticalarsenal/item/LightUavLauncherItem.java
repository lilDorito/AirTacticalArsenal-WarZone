package com.synarsis.airtacticalarsenal.item;

import com.synarsis.airtacticalarsenal.client.renderer.item.LightUavLauncherItemRenderer;
import com.synarsis.airtacticalarsenal.entity.ModEntities;
import com.synarsis.airtacticalarsenal.entity.LightUavLauncherEntity;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class LightUavLauncherItem extends Item implements GeoItem {
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public LightUavLauncherItem(Properties properties) {
        super(properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private LightUavLauncherItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new LightUavLauncherItemRenderer();
                }
                return this.renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (!(level instanceof ServerLevel serverLevel) || player == null) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        Direction face = context.getClickedFace();
        BlockPos clickedPos = context.getClickedPos();
        BlockPos placePos = face == Direction.UP ? clickedPos.above() : clickedPos.relative(face);

        if (!serverLevel.isEmptyBlock(placePos)) {
            player.displayClientMessage(Component.translatable("message.ata.place.occupied"), true);
            return InteractionResult.FAIL;
        }

        BlockState support = serverLevel.getBlockState(placePos.below());
        if (!support.isFaceSturdy(serverLevel, placePos.below(), Direction.UP)) {
            player.displayClientMessage(Component.translatable("message.ata.place.need_solid"), true);
            return InteractionResult.FAIL;
        }

        AABB checkArea = new AABB(placePos).inflate(5.0);
        List<com.synarsis.airtacticalarsenal.entity.UavLauncherEntity> nearbyLaunchers = serverLevel.getEntitiesOfClass(
                com.synarsis.airtacticalarsenal.entity.UavLauncherEntity.class, checkArea, e -> e.isAlive());
        if (!nearbyLaunchers.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.ata.place.too_close_launcher"), true);
            return InteractionResult.FAIL;
        }

        LightUavLauncherEntity launcher = ModEntities.LIGHT_UAV_LAUNCHER.get().create(serverLevel);
        if (launcher == null) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = context.getItemInHand();

        launcher.moveTo(
                placePos.getX() + 0.5D,
                placePos.getY(),
                placePos.getZ() + 0.5D,
                player.getYRot(),
                0.0F);

        if (!serverLevel.noCollision(launcher)) {
            launcher.discard();
            player.displayClientMessage(Component.translatable("message.ata.place.not_enough_space"), true);
            return InteractionResult.FAIL;
        }

        launcher.isLegitSpawn = true;
        serverLevel.addFreshEntity(launcher);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }

}
