package com.synarsis.airtacticalarsenal.block;

import com.synarsis.airtacticalarsenal.block.entity.ShahedRadarBlockEntity;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.OpenRadarScreenPacket;
import com.synarsis.airtacticalarsenal.radar.ServerTerrainScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.fml.ModList;


public class ShahedRadarBlock extends Block implements EntityBlock {

    public static final boolean SBW_LOOK = ModList.get().isLoaded("superbwarfare");
    public static volatile boolean fumoModel = SBW_LOOK;

    private static final VoxelShape FUMO_SHAPE = Shapes.or(
            Block.box(1.0, 0.0, 1.0, 15.0, 6.0, 15.0),
            Block.box(6.0, 6.0, 6.0, 10.0, 58.0, 10.0));

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.BLOCK;
    }
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public ShahedRadarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWER);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return fumoModel ? FUMO_SHAPE : Shapes.block();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SBW_LOOK ? FUMO_SHAPE : Shapes.block();
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return fumoModel ? RenderShape.ENTITYBLOCK_ANIMATED : RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ShahedRadarBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            NetworkHandler.sendToPlayer(serverPlayer, new OpenRadarScreenPacket(pos, com.synarsis.airtacticalarsenal.config.ShahedConfig.isRadarDarkBackground()));
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide) {
            level.scheduleTick(pos, this, 20);
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            ServerTerrainScanner.remove(pos);
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int newPower = calculateSignalStrength(level, pos);
        int oldPower = state.getValue(POWER);

        if (newPower != oldPower) {
            level.setBlock(pos, state.setValue(POWER, newPower), 3);
            level.updateNeighborsAt(pos, this);
            for (Direction dir : Direction.values()) {
                level.updateNeighborsAt(pos.relative(dir), this);
            }
        }

        if (com.synarsis.airtacticalarsenal.config.ShahedConfig.isRadarDarkBackground()) {
            ServerTerrainScanner.remove(pos);
        } else {
            ServerTerrainScanner.getOrCreate(pos, level);
        }

        if (!level.getChunkAt(pos).getBlockEntities().containsKey(pos)) {
            level.getBlockEntity(pos);
            level.getChunkSource().blockChanged(pos);
        }

        level.scheduleTick(pos, this, 20);
    }

    public static boolean hasOpenSky(Level level, BlockPos pos) {
        if (!com.synarsis.airtacticalarsenal.config.ShahedConfig.isRadarOpenSkyRequired()) return true;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int top = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() + dx, pos.getZ() + dz);
                if (top > pos.getY() + 1) return false;
            }
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide && placer instanceof Player player
                && level.getBlockEntity(pos) instanceof com.synarsis.airtacticalarsenal.block.entity.ShahedRadarBlockEntity radar) {
            radar.setOwnerName(player.getScoreboardName());
        }
    }

    private static net.minecraft.world.scores.Team ownerTeam(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof com.synarsis.airtacticalarsenal.block.entity.ShahedRadarBlockEntity radar
                && !radar.getOwnerName().isEmpty()) {
            return level.getScoreboard().getPlayersTeam(radar.getOwnerName());
        }
        return null;
    }

    private int calculateSignalStrength(ServerLevel level, BlockPos pos) {
        if (!hasOpenSky(level, pos)) return 0;
        Vec3 radarCenter = Vec3.atCenterOf(pos);
        int range = com.synarsis.airtacticalarsenal.config.ShahedConfig.getRadarRange();
        int minAltitude = com.synarsis.airtacticalarsenal.config.ShahedConfig.getRadarMinAltitude();
        double top = Math.max(level.getMaxBuildHeight(),
                com.synarsis.airtacticalarsenal.config.ShahedConfig.getIskanderMaxFlightHeight()) + 64;
        AABB searchBox = new AABB(
            radarCenter.x - range, level.getMinBuildHeight(), radarCenter.z - range,
            radarCenter.x + range, top, radarCenter.z + range
        );
        net.minecraft.world.scores.Team team = ownerTeam(level, pos);

        double closestDistance = Double.MAX_VALUE;
        double rangeSq = (double) range * range;

        for (Entity entity : level.getEntities(null, searchBox)) {
            if (com.synarsis.airtacticalarsenal.radar.RadarContacts.classify(entity) < 0) continue;
            if (entity.getY() < minAltitude) continue;
            double dx = entity.getX() - radarCenter.x;
            double dz = entity.getZ() - radarCenter.z;
            double horizontalSq = dx * dx + dz * dz;
            if (horizontalSq > rangeSq) continue;
            if (com.synarsis.airtacticalarsenal.radar.RadarContacts.isFriendly(team, null, entity)) continue;
            double dist = Math.sqrt(horizontalSq);
            if (dist < closestDistance) closestDistance = dist;
        }

        if (closestDistance == Double.MAX_VALUE) {
            return 0;
        }

        double d = closestDistance * 1500.0 / com.synarsis.airtacticalarsenal.config.ShahedConfig.getRadarRange();
        if (d < 50) return 15;
        if (d < 100) return 14;
        if (d < 200) return 13;
        if (d < 300) return 12;
        if (d < 400) return 10;
        if (d < 600) return 8;
        if (d < 800) return 6;
        if (d < 1000) return 5;
        if (d < 1100) return 4;
        if (d < 1200) return 3;
        if (d < 1350) return 2;
        if (d < 1500) return 1;
        return 0;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }

    @Override
    public int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }
}
