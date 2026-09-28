package com.synarsis.airtacticalarsenal.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class RoundCrater {

    private static final double DEPTH_FACTOR = 0.7;
    private static final double RIM_START = 0.72;
    private static final float MAX_RESISTANCE = 100.0f;
    private static final int MAX_GROUND_SNAP = 16;

    private RoundCrater() {}

    public static void carve(ServerLevel level, @Nullable Entity source, @Nullable DamageSource damageSource,
                             Vec3 center, double radius, RandomSource random) {
        if (radius <= 0.5) return;

        center = snapToGround(level, center);

        Explosion explosion = new Explosion(level, source, damageSource, null,
                center.x, center.y, center.z, (float) radius, false, Explosion.BlockInteraction.DESTROY_WITH_DECAY);
        if (ForgeEventFactory.onExplosionStart(level, explosion)) return;

        BlockPos c = BlockPos.containing(center);
        int r = (int) Math.ceil(radius);
        double phase = random.nextDouble() * Math.PI * 2.0;
        List<BlockPos> toBlow = explosion.getToBlow();

        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                double angle = Math.atan2(dz, dx);
                double wobble = 1.0 + 0.05 * Math.sin(3 * angle + phase) + 0.03 * Math.sin(7 * angle + phase * 2);
                for (int dy = -r; dy <= r; dy++) {
                    double vy = dy < 0 ? dy / DEPTH_FACTOR : dy;
                    double d = Math.sqrt(dx * dx + vy * vy + dz * dz) / (radius * wobble);
                    if (d > 1.0) continue;
                    if (d > RIM_START && random.nextDouble() < (d - RIM_START) / (1.0 - RIM_START)) continue;

                    BlockPos p = c.offset(dx, dy, dz);
                    if (!level.isInWorldBounds(p)) continue;
                    BlockState state = level.getBlockState(p);
                    if (state.isAir()) continue;
                    if (state.getBlock() instanceof LiquidBlock) continue;
                    if (state.getDestroySpeed(level, p) < 0) continue;
                    if (state.getExplosionResistance(level, p, explosion) > MAX_RESISTANCE) continue;
                    toBlow.add(p.immutable());
                }
            }
        }

        ForgeEventFactory.onExplosionDetonate(level, explosion, new ArrayList<>(), radius * 2.0);
        explosion.finalizeExplosion(false);
    }

    private static Vec3 snapToGround(ServerLevel level, Vec3 center) {
        BlockPos c = BlockPos.containing(center);
        if (isGround(level, c)) return center;
        for (Direction dir : Direction.values()) {
            if (isGround(level, c.relative(dir))) return center;
        }
        BlockPos.MutableBlockPos p = c.mutable();
        for (int i = 1; i <= MAX_GROUND_SNAP; i++) {
            p.move(Direction.DOWN);
            if (!level.isInWorldBounds(p)) break;
            if (isGround(level, p) || !level.getFluidState(p).isEmpty()) {
                return new Vec3(center.x, p.getY() + 1.0, center.z);
            }
        }
        return center;
    }

    private static boolean isGround(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.is(BlockTags.LEAVES) || state.is(BlockTags.REPLACEABLE)) return false;
        return !state.getCollisionShape(level, pos).isEmpty();
    }
}
