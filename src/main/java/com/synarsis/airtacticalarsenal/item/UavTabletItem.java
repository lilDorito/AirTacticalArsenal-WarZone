package com.synarsis.airtacticalarsenal.item;

import com.synarsis.airtacticalarsenal.api.DroneVariants;
import com.synarsis.airtacticalarsenal.api.UavWeight;
import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.entity.UavLauncherEntity;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.OpenUavTabletPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class UavTabletItem extends Item {

    private static final String TAG_LAUNCHERS = "LinkedLaunchers";
    private static final String TAG_STRIKE_COORDS = "StrikeCoords";
    private static final String TAG_LINKED_ORLAN = "LinkedOrlanId";
    private static final String TAG_CAMERA_MODE = "CameraMode";

    public static final int MAX_LAUNCHERS = 10;

    public UavTabletItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Nullable
    public static ItemStack heldTablet(Player player) {
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof UavTabletItem) {
                return stack;
            }
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (!getLinkedLaunchers(stack).isEmpty()) {
                clearAllLaunchers(stack);
                if (!level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.ata.tablet.links_reset"), true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            open(serverPlayer, stack);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        return InteractionResultHolder.pass(stack);
    }

    public static void open(ServerPlayer player, ItemStack stack) {
        open(player, stack, false);
    }

    public static void open(ServerPlayer player, ItemStack stack, boolean refresh) {
        if (!(stack.getItem() instanceof UavTabletItem)) {
            return;
        }
        Level level = player.level();
        List<BlockPos> linked = getLinkedLaunchers(stack);
        List<OpenUavTabletPacket.LauncherInfo> launchers = new ArrayList<>();
        List<BlockPos> valid = new ArrayList<>();
        for (BlockPos pos : linked) {
            UavLauncherEntity launcher = findLauncher(level, pos);
            if (launcher == null) {
                continue;
            }
            valid.add(pos);
            String nameKey = launcher.hasDrone()
                    ? DroneVariants.nameKey(launcher.getDroneKind(),
                    launcher.getDroneRole() == UavLauncherEntity.ROLE_STRIKE ? "drone.ata.shahed" : "drone.ata.orlan")
                    : "";
            launchers.add(new OpenUavTabletPacket.LauncherInfo(
                    launcher.blockPosition(),
                    launcher.getYRot(),
                    launcher.hasDrone() ? launcher.getDroneRole() : UavLauncherEntity.ROLE_NONE,
                    nameKey,
                    launcher.weight() == UavWeight.HEAVY,
                    launcher.hasSavedRoute() ? new ArrayList<>(launcher.getSavedRoute()) : new ArrayList<>(),
                    launcher.hasDrone() ? launcher.getDroneKind() : ""));
        }
        if (valid.size() != linked.size()) {
            setLinkedLaunchers(stack, valid);
        }

        List<OpenUavTabletPacket.FlyingInfo> flying = new ArrayList<>();
        for (OrlanEntity orlan : OrlanEntity.getActiveOrlansByOwner(player.getUUID())) {
            if (orlan.level() != level) {
                continue;
            }
            OrlanEntity.FlightPhase reconPhase = orlan.getFlightPhase();
            BlockPos target = reconPhase == OrlanEntity.FlightPhase.LANDED ? orlan.blockPosition()
                    : reconPhase == OrlanEntity.FlightPhase.RETURNING || reconPhase == OrlanEntity.FlightPhase.LANDING
                    && orlan.getLauncherPos() != null ? orlan.getLauncherPos() : orlan.getTargetPos();
            flying.add(new OpenUavTabletPacket.FlyingInfo(orlan.getId(), true, orlan.getDroneNameKey(),
                    (int) orlan.getX(), (int) orlan.getZ(),
                    target != null ? target.getX() : (int) orlan.getX(), target != null ? target.getZ() : (int) orlan.getZ(),
                    orlan.getFlightPhase().ordinal(), orlan.getRemainingFlightTicks(), true, DroneVariants.kindOf(orlan)));
        }
        int parkedId = -2;
        for (OrlanEntity.LandedDrone parked : OrlanEntity.parkedByOwner(level, player.getUUID())) {
            flying.add(new OpenUavTabletPacket.FlyingInfo(parkedId--, true, parked.nameKey(), parked.x(), parked.z(),
                    parked.x(), parked.z(), OrlanEntity.FlightPhase.LANDED.ordinal(), 0, false, parked.kind()));
        }
        for (ShahedEntity shahed : ShahedEntity.activeByOwner(level, player.getUUID())) {
            BlockPos strikeTarget = shahed.getStrikeTarget();
            flying.add(new OpenUavTabletPacket.FlyingInfo(shahed.getId(), false, shahed.getDroneNameKey(),
                    (int) shahed.getX(), (int) shahed.getZ(), strikeTarget.getX(), strikeTarget.getZ(),
                    shahed.getFlightPhase().ordinal(), shahed.remainingRoute(), shahed.hasCamera(), DroneVariants.kindOf(shahed)));
        }

        NetworkHandler.sendToPlayer(player, new OpenUavTabletPacket(launchers, flying, refresh));
    }

    public static void saveStrikeCoords(ServerPlayer player, ItemStack stack, Map<Integer, int[]> byIndex) {
        if (!(stack.getItem() instanceof UavTabletItem)) {
            return;
        }
        List<BlockPos> order = new ArrayList<>();
        for (BlockPos pos : getLinkedLaunchers(stack)) {
            UavLauncherEntity launcher = findLauncher(player.level(), pos);
            if (launcher != null && launcher.servesStrike()) {
                order.add(launcher.blockPosition());
            }
        }
        Map<BlockPos, int[]> stored = getStrikeCoords(stack);
        for (Map.Entry<Integer, int[]> entry : byIndex.entrySet()) {
            int index = entry.getKey();
            if (index >= 0 && index < order.size()) {
                stored.put(order.get(index), entry.getValue());
            }
        }
        CompoundTag coordsTag = new CompoundTag();
        for (Map.Entry<BlockPos, int[]> entry : stored.entrySet()) {
            BlockPos pos = entry.getKey();
            coordsTag.putIntArray(pos.getX() + "," + pos.getY() + "," + pos.getZ(), entry.getValue());
        }
        stack.getOrCreateTag().put(TAG_STRIKE_COORDS, coordsTag);
    }

    private static Map<BlockPos, int[]> getStrikeCoords(ItemStack stack) {
        Map<BlockPos, int[]> result = new HashMap<>();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_STRIKE_COORDS)) {
            return result;
        }
        CompoundTag coordsTag = tag.getCompound(TAG_STRIKE_COORDS);
        for (String key : coordsTag.getAllKeys()) {
            String[] parts = key.split(",");
            int[] value = coordsTag.getIntArray(key);
            if (parts.length != 3 || value.length < 2) {
                continue;
            }
            try {
                result.put(new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2])),
                        new int[]{value[0], value[1]});
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    public static boolean addLinkedLauncher(ItemStack stack, UavLauncherEntity launcher) {
        List<BlockPos> launchers = getLinkedLaunchers(stack);
        BlockPos pos = launcher.blockPosition();
        for (BlockPos existing : launchers) {
            if (existing.equals(pos)) {
                return false;
            }
        }
        if (launchers.size() >= MAX_LAUNCHERS) {
            return false;
        }
        launchers.add(pos);
        setLinkedLaunchers(stack, launchers);
        return true;
    }

    public static boolean removeLinkedLauncher(ItemStack stack, BlockPos pos) {
        List<BlockPos> launchers = getLinkedLaunchers(stack);
        boolean removed = launchers.removeIf(p -> p.equals(pos));
        if (removed) {
            setLinkedLaunchers(stack, launchers);
        }
        return removed;
    }

    public static void forgetLauncher(UavLauncherEntity gone) {
        if (gone.getServer() == null) {
            return;
        }
        Level level = gone.level();
        BlockPos at = gone.blockPosition();
        for (ServerPlayer player : gone.getServer().getPlayerList().getPlayers()) {
            boolean changed = false;
            List<ItemStack> stacks = new ArrayList<>(player.getInventory().items);
            stacks.addAll(player.getInventory().offhand);
            for (ItemStack stack : stacks) {
                if (!(stack.getItem() instanceof UavTabletItem)) {
                    continue;
                }
                List<BlockPos> linked = getLinkedLaunchers(stack);
                boolean removed = linked.removeIf(pos -> pos.equals(at)
                        || (player.level() == level && pos.distSqr(at) <= 12 && otherLauncher(level, pos, gone) == null));
                if (removed) {
                    setLinkedLaunchers(stack, linked);
                    changed = true;
                }
            }
            if (changed) {
                ItemStack held = heldTablet(player);
                if (held != null) {
                    open(player, held, true);
                }
            }
        }
    }

    private static UavLauncherEntity otherLauncher(Level level, BlockPos pos, UavLauncherEntity except) {
        for (UavLauncherEntity launcher : level.getEntitiesOfClass(UavLauncherEntity.class, new AABB(pos).inflate(2.0), UavLauncherEntity::isAlive)) {
            if (launcher != except) {
                return launcher;
            }
        }
        return null;
    }

    public static List<BlockPos> getLinkedLaunchers(ItemStack stack) {
        List<BlockPos> result = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_LAUNCHERS)) {
            ListTag listTag = tag.getList(TAG_LAUNCHERS, 10);
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag posTag = listTag.getCompound(i);
                result.add(new BlockPos(posTag.getInt("x"), posTag.getInt("y"), posTag.getInt("z")));
            }
        }
        return result;
    }

    public static void setLinkedLaunchers(ItemStack stack, List<BlockPos> launchers) {
        CompoundTag tag = stack.getOrCreateTag();
        ListTag listTag = new ListTag();
        for (BlockPos pos : launchers) {
            CompoundTag posTag = new CompoundTag();
            posTag.putInt("x", pos.getX());
            posTag.putInt("y", pos.getY());
            posTag.putInt("z", pos.getZ());
            listTag.add(posTag);
        }
        tag.put(TAG_LAUNCHERS, listTag);
    }

    public static void clearAllLaunchers(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(TAG_LAUNCHERS);
            tag.remove(TAG_STRIKE_COORDS);
            tag.remove("SavedCoords");
        }
    }

    public static int getLinkedOrlanId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getInt(TAG_LINKED_ORLAN) : -1;
    }

    public static void setLinkedOrlanId(ItemStack stack, int entityId) {
        stack.getOrCreateTag().putInt(TAG_LINKED_ORLAN, entityId);
    }

    public static boolean isCameraMode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_CAMERA_MODE);
    }

    public static void setCameraMode(ItemStack stack, boolean mode) {
        stack.getOrCreateTag().putBoolean(TAG_CAMERA_MODE, mode);
    }

    @Nullable
    public static UavLauncherEntity findLauncher(Level level, BlockPos pos) {
        List<UavLauncherEntity> launchers = level.getEntitiesOfClass(UavLauncherEntity.class,
                new AABB(pos).inflate(2.0), UavLauncherEntity::isAlive);
        UavLauncherEntity best = null;
        double bestDist = Double.MAX_VALUE;
        Vec3 center = Vec3.atBottomCenterOf(pos);
        for (UavLauncherEntity launcher : launchers) {
            double dist = launcher.position().distanceToSqr(center);
            if (dist < bestDist) {
                bestDist = dist;
                best = launcher;
            }
        }
        return best;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        List<BlockPos> launchers = getLinkedLaunchers(stack);
        if (!launchers.isEmpty()) {
            tooltip.add(Component.translatable("tooltip.ata.tablet.linked", launchers.size(), MAX_LAUNCHERS));
            for (int i = 0; i < Math.min(launchers.size(), 3); i++) {
                BlockPos pos = launchers.get(i);
                tooltip.add(Component.literal("§7  " + (i + 1) + ". [" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]"));
            }
            if (launchers.size() > 3) {
                tooltip.add(Component.translatable("tooltip.ata.tablet.and_more", launchers.size() - 3));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.ata.tablet.none_linked"));
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isCameraMode(stack);
    }
}
