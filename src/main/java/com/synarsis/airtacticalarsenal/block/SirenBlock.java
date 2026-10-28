package com.synarsis.airtacticalarsenal.block;

import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.SirenSoundPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.HashMap;
import java.util.Map;

public class SirenBlock extends Block {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Long>> STARTED = new HashMap<>();
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public SirenBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean isMoving) {
        if (level instanceof ServerLevel server) {
            boolean powered = level.hasNeighborSignal(pos);
            boolean wasPowered = state.getValue(POWERED);

            if (powered != wasPowered) {
                level.setBlock(pos, state.setValue(POWERED, powered), 3);

                if (powered) {
                    start(server, pos);
                } else {
                    allClear(server, pos);
                }
            }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(POWERED)) return;
        if (!startedAt(level).containsKey(pos)) {
            start(level, pos);
        }
    }

    private void start(ServerLevel level, BlockPos pos) {
        NetworkHandler.sendToDimension(level, new SirenSoundPacket(pos));
        startedAt(level).put(pos.immutable(), level.getGameTime());
    }

    private static void allClear(ServerLevel level, BlockPos pos) {
        boolean sounded = startedAt(level).remove(pos) != null;
        NetworkHandler.sendToDimension(level, new SirenSoundPacket(pos, sounded ? SirenSoundPacket.ALL_CLEAR : SirenSoundPacket.STOP));
    }

    private static void stop(ServerLevel level, BlockPos pos) {
        startedAt(level).remove(pos);
        NetworkHandler.sendToDimension(level, new SirenSoundPacket(pos, true));
    }

    private static Map<BlockPos, Long> startedAt(ServerLevel level) {
        return STARTED.computeIfAbsent(level.dimension(), k -> new HashMap<>());
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!oldState.is(state.getBlock())) {
            if (level instanceof ServerLevel server && state.getValue(POWERED)) {
                startedAt(server).remove(pos);
                level.scheduleTick(pos, this, 1);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            if (level instanceof ServerLevel server) {
                stop(server, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }
}
