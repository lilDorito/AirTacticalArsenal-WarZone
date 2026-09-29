package com.synarsis.airtacticalarsenal.command;

import com.synarsis.airtacticalarsenal.config.ShahedConfig;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.entity.IskanderEntity;
import com.synarsis.airtacticalarsenal.worldmap.WorldMapData;
import com.synarsis.airtacticalarsenal.worldmap.WorldMapScanner;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;

public class ShahedCommand {

    private static Component onOff(boolean value) {
        return value ? Component.translatable("command.ata.config.on").withStyle(s -> s.withColor(0x55FF55))
                : Component.translatable("command.ata.config.off").withStyle(s -> s.withColor(0xFF5555));
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ata")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("attack")
                        .then(Commands.literal("shahed")
                                .then(Commands.argument("start", Vec3Argument.vec3())
                                        .then(Commands.argument("target", BlockPosArgument.blockPos())
                                                .then(Commands.literal("low")
                                                        .executes(context -> {
                                                            Vec3 startPos = Vec3Argument.getVec3(context, "start");
                                                            BlockPos targetPos = BlockPosArgument.getBlockPos(context,
                                                                    "target");
                                                            ServerLevel level = context.getSource().getLevel();
                                                            preloadChunks(level, startPos, Vec3.atCenterOf(targetPos));
                                                            ShahedEntity shahed = new ShahedEntity(level, startPos,
                                                                    targetPos, "low");
                                                            shahed.isLegitSpawn = true;
                                                            level.addFreshEntity(shahed);
                                                            context.getSource()
                                                                    .sendSuccess(
                                                                            () -> Component
                                                                                    .translatable(
                                                                                            "command.ata.shahed.low")
                                                                                    .withStyle(
                                                                                            s -> s.withColor(0x55FF55)),
                                                                            true);
                                                            return 1;
                                                        }))
                                                .then(Commands.literal("high")
                                                        .executes(context -> {
                                                            Vec3 startPos = Vec3Argument.getVec3(context, "start");
                                                            BlockPos targetPos = BlockPosArgument.getBlockPos(context,
                                                                    "target");
                                                            ServerLevel level = context.getSource().getLevel();
                                                            preloadChunks(level, startPos, Vec3.atCenterOf(targetPos));
                                                            ShahedEntity shahed = new ShahedEntity(level, startPos,
                                                                    targetPos, "high");
                                                            shahed.isLegitSpawn = true;
                                                            level.addFreshEntity(shahed);
                                                            context.getSource()
                                                                    .sendSuccess(
                                                                            () -> Component
                                                                                    .translatable(
                                                                                            "command.ata.shahed.high")
                                                                                    .withStyle(
                                                                                            s -> s.withColor(0x55FF55)),
                                                                            true);
                                                            return 1;
                                                        })))))
                        .then(Commands.literal("iskander")
                                .then(Commands.argument("start", Vec3Argument.vec3())
                                        .then(Commands.argument("target", BlockPosArgument.blockPos())
                                                .executes(context -> {
                                                    Vec3 startPos = Vec3Argument.getVec3(context, "start");
                                                    BlockPos targetPos = BlockPosArgument.getBlockPos(context,
                                                            "target");
                                                    ServerLevel level = context.getSource().getLevel();
                                                    preloadChunks(level, startPos, Vec3.atCenterOf(targetPos));
                                                    IskanderEntity iskander = new IskanderEntity(level, startPos,
                                                            targetPos);
                                                    iskander.isLegitSpawn = true;
                                                    level.addFreshEntity(iskander);
                                                    context.getSource().sendSuccess(() -> Component.translatable(
                                                            "command.ata.iskander.launched",
                                                            targetPos.getX(), targetPos.getZ()),
                                                            true);
                                                    return 1;
                                                })))))
                .then(Commands.literal("config")
                        .executes(context -> {
                            context.getSource().sendSuccess(() -> Component.translatable("command.ata.config.title")
                                    .withStyle(s -> s.withColor(0xFFFF55)), false);
                            context.getSource()
                                    .sendSuccess(() -> Component
                                            .translatable("command.ata.config.target_y",
                                                    Component.literal(String.valueOf(ShahedConfig.TARGET_Y_LEVEL.get()))
                                                            .withStyle(s -> s.withColor(0x55FFFF)))
                                            .withStyle(s -> s.withColor(0xAAAAAA)), false);
                            context.getSource()
                                    .sendSuccess(() -> Component
                                            .translatable("command.ata.config.particles",
                                                    onOff(ShahedConfig.areExplosionParticlesEnabled()))
                                            .withStyle(s -> s.withColor(0xAAAAAA)), false);
                            context.getSource()
                                    .sendSuccess(
                                            () -> Component
                                                    .translatable("command.ata.config.particle_mult",
                                                            Component
                                                                    .literal(String.valueOf(
                                                                            ShahedConfig.getParticleMultiplier()))
                                                                    .withStyle(s -> s.withColor(0x55FFFF)))
                                                    .withStyle(s -> s.withColor(0xAAAAAA)),
                                            false);
                            context.getSource()
                                    .sendSuccess(() -> Component
                                            .translatable("command.ata.config.block_damage",
                                                    onOff(ShahedConfig.doExplosionsBreakBlocks()))
                                            .withStyle(s -> s.withColor(0xAAAAAA)), false);
                            context.getSource()
                                    .sendSuccess(() -> Component
                                            .translatable("command.ata.config.sounds",
                                                    onOff(ShahedConfig.areCustomExplosionSoundsEnabled()))
                                            .withStyle(s -> s.withColor(0xAAAAAA)), false);
                            context.getSource()
                                    .sendSuccess(() -> Component
                                            .translatable("command.ata.config.blacklist",
                                                    onOff(ShahedConfig.isBlacklistEnabled()))
                                            .withStyle(s -> s.withColor(0xAAAAAA)), false);
                            context.getSource()
                                    .sendSuccess(() -> Component
                                            .translatable("command.ata.config.whitelist",
                                                    onOff(ShahedConfig.isWhitelistEnabled()))
                                            .withStyle(s -> s.withColor(0xAAAAAA)), false);
                            context.getSource()
                                    .sendSuccess(() -> Component
                                            .translatable("command.ata.config.logging",
                                                    onOff(ShahedConfig.isLoggingEnabled()))
                                            .withStyle(s -> s.withColor(0xAAAAAA)), false);
                            return 1;
                        })
                        .then(Commands.literal("targetY")
                                .then(Commands.argument("value", IntegerArgumentType.integer(-64, 320))
                                        .executes(context -> {
                                            int value = IntegerArgumentType.getInteger(context, "value");
                                            ShahedConfig.TARGET_Y_LEVEL.set(value);
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.target_y.set", value)
                                                            .withStyle(s -> s.withColor(0x55FF55)), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("particles")
                                .executes(context -> {
                                    context.getSource()
                                            .sendSuccess(() -> Component
                                                    .translatable("command.ata.config.particles",
                                                            onOff(ShahedConfig.areExplosionParticlesEnabled()))
                                                    .withStyle(s -> s.withColor(0xFFFF55)), false);
                                    context.getSource()
                                            .sendSuccess(() -> Component
                                                    .translatable("command.ata.config.particle_mult",
                                                            ShahedConfig.getParticleMultiplier())
                                                    .withStyle(s -> s.withColor(0xFFFF55)), false);
                                    return 1;
                                })
                                .then(Commands.literal("enable")
                                        .executes(context -> {
                                            ShahedConfig.setExplosionParticlesEnabled(true);
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.particles.enabled")
                                                            .withStyle(s -> s.withColor(0x55FF55)), true);
                                            return 1;
                                        }))
                                .then(Commands.literal("disable")
                                        .executes(context -> {
                                            ShahedConfig.setExplosionParticlesEnabled(false);
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.particles.disabled")
                                                            .withStyle(s -> s.withColor(0xFF5555)), true);
                                            return 1;
                                        }))
                                .then(Commands.literal("multiplier")
                                        .then(Commands.argument("value", IntegerArgumentType.integer(0, 500))
                                                .executes(context -> {
                                                    int value = IntegerArgumentType.getInteger(context, "value");
                                                    ShahedConfig.setParticleMultiplier(value);
                                                    context.getSource()
                                                            .sendSuccess(() -> Component.translatable(
                                                                    "command.ata.config.particle_mult.set", value)
                                                                    .withStyle(s -> s.withColor(0x55FF55)), true);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("blockDamage")
                                .executes(context -> {
                                    context.getSource()
                                            .sendSuccess(() -> Component
                                                    .translatable("command.ata.config.block_damage",
                                                            onOff(ShahedConfig.doExplosionsBreakBlocks()))
                                                    .withStyle(s -> s.withColor(0xFFFF55)), false);
                                    return 1;
                                })
                                .then(Commands.literal("enable")
                                        .executes(context -> {
                                            ShahedConfig.setExplosionsBreakBlocks(true);
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.block_damage.enabled")
                                                            .withStyle(s -> s.withColor(0x55FF55)), true);
                                            return 1;
                                        }))
                                .then(Commands.literal("disable")
                                        .executes(context -> {
                                            ShahedConfig.setExplosionsBreakBlocks(false);
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.block_damage.disabled")
                                                            .withStyle(s -> s.withColor(0xFF5555)), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("sounds")
                                .executes(context -> {
                                    context.getSource()
                                            .sendSuccess(
                                                    () -> Component
                                                            .translatable("command.ata.config.sounds",
                                                                    onOff(ShahedConfig
                                                                            .areCustomExplosionSoundsEnabled()))
                                                            .withStyle(s -> s.withColor(0xFFFF55)),
                                                    false);
                                    return 1;
                                })
                                .then(Commands.literal("enable")
                                        .executes(context -> {
                                            ShahedConfig.setCustomExplosionSoundsEnabled(true);
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.sounds.enabled")
                                                            .withStyle(s -> s.withColor(0x55FF55)), true);
                                            return 1;
                                        }))
                                .then(Commands.literal("disable")
                                        .executes(context -> {
                                            ShahedConfig.setCustomExplosionSoundsEnabled(false);
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.sounds.disabled")
                                                            .withStyle(s -> s.withColor(0xFF5555)), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("logging")
                                .executes(context -> {
                                    context.getSource()
                                            .sendSuccess(() -> Component
                                                    .translatable("command.ata.config.logging",
                                                            onOff(ShahedConfig.isLoggingEnabled()))
                                                    .withStyle(s -> s.withColor(0xFFFF55)), false);
                                    return 1;
                                })
                                .then(Commands.literal("enable")
                                        .executes(context -> {
                                            ShahedConfig.setLoggingEnabled(true);
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.logging.enabled")
                                                            .withStyle(s -> s.withColor(0x55FF55)), true);
                                            return 1;
                                        }))
                                .then(Commands.literal("disable")
                                        .executes(context -> {
                                            ShahedConfig.setLoggingEnabled(false);
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.logging.disabled")
                                                            .withStyle(s -> s.withColor(0xFF5555)), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("zone")
                                .executes(context -> {
                                    context.getSource()
                                            .sendSuccess(() -> Component
                                                    .translatable("command.ata.config.blacklist",
                                                            onOff(ShahedConfig.isBlacklistEnabled()))
                                                    .withStyle(s -> s.withColor(0xFFFF55)), false);
                                    context.getSource()
                                            .sendSuccess(() -> Component
                                                    .translatable("command.ata.config.whitelist",
                                                            onOff(ShahedConfig.isWhitelistEnabled()))
                                                    .withStyle(s -> s.withColor(0xFFFF55)), false);
                                    return 1;
                                })
                                .then(Commands.literal("blacklist")
                                        .executes(context -> {
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.blacklist",
                                                                    onOff(ShahedConfig.isBlacklistEnabled()))
                                                            .withStyle(s -> s.withColor(0xFFFF55)), false);
                                            return 1;
                                        })
                                        .then(Commands.literal("enable")
                                                .executes(context -> {
                                                    ShahedConfig.setBlacklistEnabled(true);
                                                    context.getSource()
                                                            .sendSuccess(() -> Component
                                                                    .translatable(
                                                                            "command.ata.config.blacklist.enabled")
                                                                    .withStyle(s -> s.withColor(0x55FF55)), true);
                                                    return 1;
                                                }))
                                        .then(Commands.literal("disable")
                                                .executes(context -> {
                                                    ShahedConfig.setBlacklistEnabled(false);
                                                    context.getSource()
                                                            .sendSuccess(() -> Component
                                                                    .translatable(
                                                                            "command.ata.config.blacklist.disabled")
                                                                    .withStyle(s -> s.withColor(0xFF5555)), true);
                                                    return 1;
                                                })))
                                .then(Commands.literal("whitelist")
                                        .executes(context -> {
                                            context.getSource()
                                                    .sendSuccess(() -> Component
                                                            .translatable("command.ata.config.whitelist",
                                                                    onOff(ShahedConfig.isWhitelistEnabled()))
                                                            .withStyle(s -> s.withColor(0xFFFF55)), false);
                                            return 1;
                                        })
                                        .then(Commands.literal("enable")
                                                .executes(context -> {
                                                    ShahedConfig.setWhitelistEnabled(true);
                                                    context.getSource()
                                                            .sendSuccess(() -> Component
                                                                    .translatable(
                                                                            "command.ata.config.whitelist.enabled")
                                                                    .withStyle(s -> s.withColor(0x55FF55)), true);
                                                    return 1;
                                                }))
                                        .then(Commands.literal("disable")
                                                .executes(context -> {
                                                    ShahedConfig.setWhitelistEnabled(false);
                                                    context.getSource()
                                                            .sendSuccess(() -> Component
                                                                    .translatable(
                                                                            "command.ata.config.whitelist.disabled")
                                                                    .withStyle(s -> s.withColor(0xFF5555)), true);
                                                    return 1;
                                                }))))
                        .then(Commands.literal("reload")
                                .executes(context -> {
                                    ShahedConfig.reload();
                                    context.getSource()
                                            .sendSuccess(() -> Component.translatable("command.ata.config.reload")
                                                    .withStyle(s -> s.withColor(0x55FF55)), true);
                                    return 1;
                                })))
                .then(Commands.literal("terrain")
                        .then(Commands.literal("scan")
                                .executes(ctx -> startTerrainScan(ctx.getSource(), 10000, 0, 0, false))
                                .then(Commands.argument("size", IntegerArgumentType.integer(512, 100000))
                                        .executes(ctx -> startTerrainScan(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "size"), 0, 0, false))
                                        .then(Commands.argument("centerX", IntegerArgumentType.integer())
                                                .then(Commands.argument("centerZ", IntegerArgumentType.integer())
                                                        .executes(ctx -> startTerrainScan(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "size"),
                                                                IntegerArgumentType.getInteger(ctx, "centerX"),
                                                                IntegerArgumentType.getInteger(ctx, "centerZ"), false))))))
                        .then(Commands.literal("rescan")
                                .executes(ctx -> startTerrainScan(ctx.getSource(), 10000, 0, 0, true))
                                .then(Commands.argument("size", IntegerArgumentType.integer(512, 100000))
                                        .executes(ctx -> startTerrainScan(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "size"), 0, 0, true))
                                        .then(Commands.argument("centerX", IntegerArgumentType.integer())
                                                .then(Commands.argument("centerZ", IntegerArgumentType.integer())
                                                        .executes(ctx -> startTerrainScan(ctx.getSource(),
                                                                IntegerArgumentType.getInteger(ctx, "size"),
                                                                IntegerArgumentType.getInteger(ctx, "centerX"),
                                                                IntegerArgumentType.getInteger(ctx, "centerZ"), true))))))
                        .then(Commands.literal("status")
                                .executes(ctx -> {
                                    ServerLevel overworld = ctx.getSource().getServer().overworld();
                                    int tiles = com.synarsis.airtacticalarsenal.radar.TerrainTileStore.count(overworld);
                                    if (com.synarsis.airtacticalarsenal.radar.WorldTerrainScan.isActive()) {
                                        int pct = com.synarsis.airtacticalarsenal.radar.WorldTerrainScan.percent();
                                        ctx.getSource().sendSuccess(() -> Component.translatable("command.ata.terrain.running", pct, tiles)
                                                .withStyle(st -> st.withColor(0xFFFF55)), false);
                                    } else {
                                        ctx.getSource().sendSuccess(() -> Component.translatable("command.ata.terrain.idle", tiles)
                                                .withStyle(st -> st.withColor(0x55FF55)), false);
                                    }
                                    return 1;
                                }))
                        .then(Commands.literal("cancel")
                                .executes(ctx -> {
                                    boolean was = com.synarsis.airtacticalarsenal.radar.WorldTerrainScan.cancel();
                                    ctx.getSource().sendSuccess(() -> Component.translatable(
                                            was ? "command.ata.terrain.cancelled" : "command.ata.terrain.not_running")
                                            .withStyle(st -> st.withColor(was ? 0xFFAA00 : 0xFF5555)), true);
                                    return was ? 1 : 0;
                                })))
                .then(Commands.literal("map")
                        .then(Commands.literal("rescan")
                                .executes(context -> {
                                    ServerLevel level = context.getSource().getLevel();
                                    WorldMapData mapData = WorldMapData.get(level);
                                    mapData.clear();
                                    WorldMapScanner.getInstance().startScan(level);
                                    context.getSource().sendSuccess(() -> Component.translatable("command.ata.map.rescan_started")
                                            .withStyle(s -> s.withColor(0x55FF55)), true);
                                    return 1;
                                }))
                        .then(Commands.literal("status")
                                .executes(context -> {
                                    WorldMapScanner scanner = WorldMapScanner.getInstance();
                                    ServerLevel level = context.getSource().getLevel();
                                    WorldMapData mapData = WorldMapData.get(level);

                                    if (scanner.isScanning()) {
                                        context.getSource().sendSuccess(() -> Component.literal(
                                                String.format("Scanning: %d/%d chunks (%.1f%%)",
                                                        scanner.getProgress(), scanner.getTotalChunks(),
                                                        scanner.getProgressPercent()))
                                                .withStyle(s -> s.withColor(0xFFFF55)), false);
                                    } else if (mapData.isScanComplete()) {
                                        context.getSource().sendSuccess(() -> Component.literal(
                                                String.format("Scan complete: %d coastline points",
                                                        mapData.getCoastlinePointCount()))
                                                .withStyle(s -> s.withColor(0x55FF55)), false);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.translatable("command.ata.map.not_scanned")
                                                .withStyle(s -> s.withColor(0xFF5555)), false);
                                    }
                                    return 1;
                                }))));
    }

    private static int startTerrainScan(CommandSourceStack source, int size, int centerX, int centerZ, boolean overwrite) {
        if (com.synarsis.airtacticalarsenal.radar.WorldTerrainScan.isActive()) {
            int pct = com.synarsis.airtacticalarsenal.radar.WorldTerrainScan.percent();
            source.sendFailure(Component.translatable("command.ata.terrain.busy", pct));
            return 0;
        }
        int tiles = com.synarsis.airtacticalarsenal.radar.WorldTerrainScan.start(size, centerX, centerZ, overwrite, source);
        source.sendSuccess(() -> Component.translatable("command.ata.terrain.started", size, size, centerX, centerZ, tiles)
                .withStyle(st -> st.withColor(0x55FF55)), true);
        return 1;
    }

    private static void preloadChunks(ServerLevel level, Vec3 from, Vec3 to) {
        ChunkPos spawnChunk = new ChunkPos(new BlockPos((int) from.x, (int) from.y, (int) from.z));
        for (int x = -3; x <= 3; ++x) {
            for (int z = -3; z <= 3; ++z) {
                level.setChunkForced(spawnChunk.x + x, spawnChunk.z + z, true);
            }
        }

        double totalDistance = from.distanceTo(to);
        int numPoints = Math.max(1, (int) (totalDistance / 32.0));
        for (int i = 0; i <= numPoints; ++i) {
            double progress = (double) i / (double) numPoints;
            Vec3 pointOnPath = new Vec3(
                    from.x + (to.x - from.x) * progress,
                    from.y + (to.y - from.y) * progress,
                    from.z + (to.z - from.z) * progress);
            ChunkPos pathChunk = new ChunkPos(
                    new BlockPos((int) pointOnPath.x, (int) pointOnPath.y, (int) pointOnPath.z));
            for (int x = -2; x <= 2; ++x) {
                for (int z = -2; z <= 2; ++z) {
                    level.setChunkForced(pathChunk.x + x, pathChunk.z + z, true);
                }
            }
        }
    }
}
