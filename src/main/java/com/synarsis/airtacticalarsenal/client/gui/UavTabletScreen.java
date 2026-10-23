package com.synarsis.airtacticalarsenal.client.gui;

import com.synarsis.airtacticalarsenal.client.ClientDroneNames;
import com.synarsis.airtacticalarsenal.client.ClientRouteLimits;
import com.synarsis.airtacticalarsenal.client.OrlanCameraHandler;
import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.entity.UavLauncherEntity;
import com.synarsis.airtacticalarsenal.network.LaunchFromReconUavTabletPacket;
import com.synarsis.airtacticalarsenal.network.LaunchShahedByRoutePacket;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.OpenUavTabletPacket;
import com.synarsis.airtacticalarsenal.network.OrlanCameraViewPacket;
import com.synarsis.airtacticalarsenal.network.OrlanReturnPacket;
import com.synarsis.airtacticalarsenal.network.RefreshUavTabletPacket;
import com.synarsis.airtacticalarsenal.network.RemoveLauncherFromTabletPacket;
import com.synarsis.airtacticalarsenal.network.UpdateOrlanRoutePacket;
import com.synarsis.airtacticalarsenal.util.Tr;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@OnlyIn(Dist.CLIENT)
public class UavTabletScreen extends AtaScaledScreen {

    private static final int GUI_WIDTH = 440;
    private static final int GUI_HEIGHT = 370;
    private static final int BORDER_COLOR = 0xFF2E7D32;
    private static final int ACCENT_COLOR = 0xFF66BB6A;
    private static final int BG_COLOR = 0xE0101010;
    private static final int MAX_VISIBLE_LAUNCHERS = 6;
    private static final int MAX_VISIBLE_DRONES = 5;
    private static final int LIST_TOP = 40;
    private static final int DIVIDER_Y = 206;
    private static final int DRONES_TOP = 226;
    private static final int REFRESH_TICKS = 40;

    private final List<OpenUavTabletPacket.LauncherInfo> launchers = new ArrayList<>();
    private final List<OpenUavTabletPacket.FlyingInfo> flying = new ArrayList<>();
    private final List<List<BlockPos>> routes = new ArrayList<>();
    private final Set<Integer> launchedLocally = new HashSet<>();

    private int selected = -1;
    private final Set<Integer> checked = new HashSet<>();
    private int scrollOffset;
    private int droneScrollOffset;

    private Button launchButton;
    private Button launchAllButton;
    private Button removeButton;
    private Button routeButton;
    private Button checkAllButton;
    private final List<Button> launcherButtons = new ArrayList<>();
    private final List<Button> droneButtons = new ArrayList<>();
    private final List<Button> returnButtons = new ArrayList<>();
    private static final float RETURN_ICON_SCALE = 2.0F;

    private final Map<Integer, Integer> localTimers = new HashMap<>();
    private int tickCounter;
    private String statusMessage = "";

    public UavTabletScreen(List<OpenUavTabletPacket.LauncherInfo> launchers, List<OpenUavTabletPacket.FlyingInfo> flying) {
        super(Component.literal(""));
        setData(launchers, flying);
    }

    @Override
    protected int designWidth() {
        return GUI_WIDTH;
    }

    @Override
    protected int designHeight() {
        return GUI_HEIGHT;
    }

    private void setData(List<OpenUavTabletPacket.LauncherInfo> newLaunchers, List<OpenUavTabletPacket.FlyingInfo> newFlying) {
        BlockPos selectedPos = selected >= 0 && selected < launchers.size() ? launchers.get(selected).pos() : null;
        Set<BlockPos> checkedPos = new HashSet<>();
        for (int idx : checked) {
            if (idx < launchers.size()) {
                checkedPos.add(launchers.get(idx).pos());
            }
        }
        launchers.clear();
        launchers.addAll(newLaunchers);
        routes.clear();
        for (OpenUavTabletPacket.LauncherInfo info : launchers) {
            routes.add(new ArrayList<>(info.route()));
        }
        flying.clear();
        flying.addAll(newFlying);
        launchedLocally.clear();
        selected = -1;
        checked.clear();
        for (int i = 0; i < launchers.size(); i++) {
            BlockPos pos = launchers.get(i).pos();
            if (pos.equals(selectedPos)) {
                selected = i;
            }
            if (checkedPos.contains(pos)) {
                checked.add(i);
            }
        }
        scrollOffset = Math.max(0, Math.min(scrollOffset, launchers.size() - MAX_VISIBLE_LAUNCHERS));
        droneScrollOffset = Math.max(0, Math.min(droneScrollOffset, flying.size() - MAX_VISIBLE_DRONES));
        localTimers.clear();
        for (OpenUavTabletPacket.FlyingInfo drone : flying) {
            if (drone.recon()) {
                localTimers.put(drone.entityId(), drone.remainingTicks());
            }
        }
    }

    public void update(List<OpenUavTabletPacket.LauncherInfo> newLaunchers, List<OpenUavTabletPacket.FlyingInfo> newFlying) {
        setData(newLaunchers, newFlying);
        if (launchButton != null) {
            rebuildLauncherButtons();
            rebuildDroneButtons();
            updateButtonStates();
            updateCheckAllButton();
        }
    }

    private int left() {
        return (this.width - GUI_WIDTH) / 2;
    }

    private int top() {
        return (this.height - GUI_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        super.init();
        int guiLeft = left();
        int guiTop = top();
        int rightX = guiLeft + 205;
        int btnW = 110;

        rebuildLauncherButtons();
        rebuildDroneButtons();

        this.checkAllButton = Button.builder(Component.translatable(allChecked() ? "gui.ata.tablet.deselect_all" : "gui.ata.tablet.select_all"),
                        button -> toggleCheckAll())
                .bounds(guiLeft + 10, guiTop + LIST_TOP + MAX_VISIBLE_LAUNCHERS * 22 + 4, 180, 18)
                .build();
        this.addWidget(this.checkAllButton);

        this.routeButton = Button.builder(Component.translatable("gui.ata.tablet.route"), button -> onRouteClicked())
                .bounds(rightX, guiTop + 130, btnW, 20)
                .build();
        this.addWidget(this.routeButton);

        this.removeButton = Button.builder(Component.translatable("gui.ata.tablet.unlink"), button -> onRemoveClicked())
                .bounds(rightX + btnW + 5, guiTop + 130, btnW, 20)
                .build();
        this.addWidget(this.removeButton);

        this.launchButton = Button.builder(Component.translatable("gui.ata.tablet.launch"), button -> onLaunchClicked())
                .bounds(rightX, guiTop + 155, btnW, 20)
                .build();
        this.addWidget(this.launchButton);

        this.launchAllButton = Button.builder(Component.translatable("gui.ata.tablet.launch_all"), button -> onLaunchAllClicked())
                .bounds(rightX + btnW + 5, guiTop + 155, btnW, 20)
                .build();
        this.addWidget(this.launchAllButton);

        updateButtonStates();
    }

    private boolean allChecked() {
        return !launchers.isEmpty() && checked.size() == launchers.size();
    }

    private boolean hasDrone(int idx) {
        return idx >= 0 && idx < launchers.size() && launchers.get(idx).hasDrone() && !launchedLocally.contains(idx);
    }

    private boolean hasRoute(int idx) {
        return idx >= 0 && idx < routes.size() && !routes.get(idx).isEmpty();
    }

    private int effectiveRole(int idx) {
        OpenUavTabletPacket.LauncherInfo info = launchers.get(idx);
        if (info.role() != UavLauncherEntity.ROLE_NONE) {
            return info.role();
        }
        return info.heavy() ? UavLauncherEntity.ROLE_STRIKE : UavLauncherEntity.ROLE_RECON;
    }

    private boolean ready(int idx) {
        return hasDrone(idx) && hasRoute(idx) && isPlayerInRange(idx);
    }

    @Override
    public boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        int guiTop = top();
        if (mouseY < guiTop + DIVIDER_Y) {
            if (launchers.size() > MAX_VISIBLE_LAUNCHERS && mouseX < left() + 195) {
                scrollOffset = Math.max(0, Math.min(scrollOffset - (int) delta, launchers.size() - MAX_VISIBLE_LAUNCHERS));
                rebuildLauncherButtons();
            }
        } else if (flying.size() > MAX_VISIBLE_DRONES) {
            droneScrollOffset = Math.max(0, Math.min(droneScrollOffset - (int) delta, flying.size() - MAX_VISIBLE_DRONES));
            rebuildDroneButtons();
        }
        return super.mouseScrolledScaled(mouseX, mouseY, delta);
    }

    private void rebuildLauncherButtons() {
        for (Button btn : launcherButtons) {
            this.removeWidget(btn);
        }
        launcherButtons.clear();
        int guiLeft = left();
        int guiTop = top();
        int visible = Math.min(launchers.size() - scrollOffset, MAX_VISIBLE_LAUNCHERS);
        for (int i = 0; i < visible; i++) {
            final int idx = i + scrollOffset;
            OpenUavTabletPacket.LauncherInfo info = launchers.get(idx);
            String check = checked.contains(idx) ? "§a[✔]" : "§7[ ]";
            String droneIcon;
            if (!hasDrone(idx)) {
                droneIcon = "§8○";
            } else if (info.role() == UavLauncherEntity.ROLE_STRIKE) {
                droneIcon = "§c●";
            } else {
                droneIcon = "§b●";
            }
            String routeIcon = hasRoute(idx) ? "§a→" : "§8-";
            String weight = info.heavy() ? "§6H" : "§eL";
            String text = check + " " + droneIcon + " " + routeIcon + " " + weight + " §f" + Tr.s("gui.ata.tablet.launcher_short", idx + 1);
            if (idx == selected) {
                text = "▶" + text;
            }
            Button btn = Button.builder(Component.literal(text), button -> onLauncherButtonClicked(idx))
                    .bounds(guiLeft + 10, guiTop + LIST_TOP + i * 22, 180, 20)
                    .build();
            launcherButtons.add(btn);
            this.addWidget(btn);
        }
    }

    private void rebuildDroneButtons() {
        for (Button btn : droneButtons) {
            this.removeWidget(btn);
        }
        droneButtons.clear();
        returnButtons.clear();
        int guiLeft = left();
        int guiTop = top();
        int visible = Math.min(flying.size() - droneScrollOffset, MAX_VISIBLE_DRONES);
        for (int i = 0; i < visible; i++) {
            final int idx = i + droneScrollOffset;
            final OpenUavTabletPacket.FlyingInfo drone = flying.get(idx);
            int y = guiTop + DRONES_TOP + i * 24;
            Button main = Button.builder(Component.literal(droneEntry(drone, idx)),
                            button -> connectCamera(drone))
                    .bounds(guiLeft + 10, y, drone.recon() ? 340 : 410, 20)
                    .build();
            main.active = drone.camera();
            droneButtons.add(main);
            this.addWidget(main);
            if (drone.recon()) {
                Button routeBtn = Button.builder(Component.literal("§6→"), button -> updateDroneRoute(drone))
                        .bounds(guiLeft + 355, y, 35, 20)
                        .build();
                droneButtons.add(routeBtn);
                this.addWidget(routeBtn);
                Button returnBtn = Button.builder(Component.empty(), button -> returnDrone(drone))
                        .bounds(guiLeft + 395, y, 35, 20)
                        .build();
                droneButtons.add(returnBtn);
                returnButtons.add(returnBtn);
                this.addWidget(returnBtn);
            }
        }
    }

    private String droneEntry(OpenUavTabletPacket.FlyingInfo drone, int idx) {
        String name = ClientDroneNames.name(drone.entityId(), drone.recon() ? "drone.ata.orlan" : "drone.ata.shahed");
        int x = drone.x();
        int z = drone.z();
        Entity entity = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(drone.entityId()) : null;
        if (entity != null) {
            x = (int) entity.getX();
            z = (int) entity.getZ();
        }
        String phase = drone.recon() ? reconPhase(drone) : strikePhase(drone.phase());
        String extra;
        if (drone.recon()) {
            int seconds = Math.max(0, localTimers.getOrDefault(drone.entityId(), drone.remainingTicks())) / 20;
            extra = String.format("§e%02d:%02d", seconds / 60, seconds % 60);
        } else {
            extra = "";
        }
        String camera = drone.camera() ? " §a" + Tr.s("gui.ata.uav_tablet.camera") : "";
        return Tr.s("gui.ata.uav_tablet.entry", name, idx + 1, x, z, phase, extra) + camera;
    }

    private String reconPhase(OpenUavTabletPacket.FlyingInfo drone) {
        int phase = drone.phase();
        int[] cached = com.synarsis.airtacticalarsenal.network.ClientPacketHandler.getCachedOrlanData(drone.entityId());
        if (cached != null) {
            phase = cached[0];
        }
        OrlanEntity.FlightPhase[] phases = OrlanEntity.FlightPhase.values();
        if (phase < 0 || phase >= phases.length) {
            return "";
        }
        return switch (phases[phase]) {
            case LAUNCHING, CLIMBING -> "§d▲";
            case CRUISING -> "§6→";
            case PATROLLING -> "§a◎";
            case RETURNING -> "§c↩";
            case LANDING -> "§e▼";
            case LANDED -> "§7■";
        };
    }

    private static String strikePhase(int phase) {
        ShahedEntity.FlightPhase[] phases = ShahedEntity.FlightPhase.values();
        if (phase < 0 || phase >= phases.length) {
            return "";
        }
        return switch (phases[phase]) {
            case WARMUP, LAUNCHING, CLIMBING -> "§d▲";
            case CRUISING -> "§6→";
            case DIVING -> "§c▼";
        };
    }

    private void onLauncherButtonClicked(int index) {
        if (Screen.hasShiftDown()) {
            if (!checked.remove(index)) {
                checked.add(index);
            }
        } else if (index >= 0 && index < launchers.size()) {
            selected = index;
            statusMessage = "";
        }
        rebuildLauncherButtons();
        updateButtonStates();
        updateCheckAllButton();
    }

    private void toggleCheckAll() {
        if (allChecked()) {
            checked.clear();
        } else {
            for (int i = 0; i < launchers.size(); i++) {
                checked.add(i);
            }
        }
        rebuildLauncherButtons();
        updateButtonStates();
        updateCheckAllButton();
    }

    private void updateCheckAllButton() {
        if (checkAllButton != null) {
            checkAllButton.setMessage(Component.translatable(allChecked() ? "gui.ata.tablet.deselect_all" : "gui.ata.tablet.select_all"));
        }
    }

    private boolean isPlayerInRange(int idx) {
        if (idx < 0 || idx >= launchers.size() || Minecraft.getInstance().player == null) {
            return false;
        }
        BlockPos pos = launchers.get(idx).pos();
        double dist = Minecraft.getInstance().player.position().distanceTo(Vec3.atCenterOf(pos));
        return dist <= ClientRouteLimits.tabletRange();
    }

    private void updateButtonStates() {
        boolean hasSelection = selected >= 0 && selected < launchers.size();
        boolean inRange = hasSelection && isPlayerInRange(selected);
        launchButton.active = hasSelection && ready(selected);
        removeButton.active = hasSelection;
        routeButton.active = hasSelection || !checked.isEmpty();
        boolean anyReady = false;
        for (int i = 0; i < launchers.size(); i++) {
            if (ready(i)) {
                anyReady = true;
                break;
            }
        }
        launchAllButton.active = anyReady;
        String tooFar = Tr.s("gui.ata.tablet.too_far", ClientRouteLimits.tabletRange());
        if (hasSelection && !inRange) {
            statusMessage = tooFar;
        } else if (statusMessage.equals(tooFar)) {
            statusMessage = "";
        }
    }

    private void onRouteClicked() {
        List<Integer> indices = new ArrayList<>();
        if (!checked.isEmpty()) {
            indices.addAll(checked);
            Collections.sort(indices);
        } else if (selected >= 0) {
            indices.add(selected);
        }
        if (indices.isEmpty()) {
            return;
        }
        int role;
        if (selected >= 0 && indices.contains(selected)) {
            role = effectiveRole(selected);
        } else {
            role = effectiveRole(indices.get(0));
        }
        List<Integer> group = new ArrayList<>();
        for (int idx : indices) {
            if (effectiveRole(idx) == role) {
                group.add(idx);
            }
        }
        statusMessage = group.size() < indices.size() ? Tr.s("gui.ata.uav_tablet.route_one_type") : "";

        List<BlockPos> positions = new ArrayList<>();
        List<Float> yaws = new ArrayList<>();
        List<Boolean> drones = new ArrayList<>();
        List<List<BlockPos>> groupRoutes = new ArrayList<>();
        for (int idx : group) {
            positions.add(launchers.get(idx).pos());
            yaws.add(launchers.get(idx).yaw());
            drones.add(hasDrone(idx));
            groupRoutes.add(new ArrayList<>(routes.get(idx)));
        }
        java.util.function.Consumer<List<List<BlockPos>>> onSave = updated -> {
            for (int i = 0; i < group.size() && i < updated.size(); i++) {
                routes.set(group.get(i), new ArrayList<>(updated.get(i)));
            }
            rebuildLauncherButtons();
            updateButtonStates();
        };
        if (role == UavLauncherEntity.ROLE_STRIKE) {
            Minecraft.getInstance().setScreen(new ShahedRouteScreen(this, positions, yaws, drones, groupRoutes, onSave));
        } else {
            Minecraft.getInstance().setScreen(new OrlanRouteScreen(this, positions, yaws, drones, groupRoutes, onSave));
        }
    }

    private void launch(int idx) {
        BlockPos pos = launchers.get(idx).pos();
        if (launchers.get(idx).role() == UavLauncherEntity.ROLE_STRIKE) {
            NetworkHandler.sendToServer(new LaunchShahedByRoutePacket(pos, 0));
        } else {
            NetworkHandler.sendToServer(new LaunchFromReconUavTabletPacket(pos, 0, 0));
        }
        launchedLocally.add(idx);
    }

    private void onLaunchClicked() {
        if (selected < 0 || selected >= launchers.size() || !ready(selected)) {
            return;
        }
        launch(selected);
        statusMessage = Tr.s("gui.ata.tablet.launch_done");
        rebuildLauncherButtons();
        updateButtonStates();
    }

    private void onLaunchAllClicked() {
        int count = 0;
        for (int i = 0; i < launchers.size(); i++) {
            if (ready(i)) {
                launch(i);
                count++;
            }
        }
        statusMessage = count == 0 ? Tr.s("gui.ata.tablet.no_ready") : Tr.s("gui.ata.tablet.launched_count", count);
        rebuildLauncherButtons();
        updateButtonStates();
    }

    private void onRemoveClicked() {
        if (selected < 0 || selected >= launchers.size()) {
            return;
        }
        int removed = selected;
        NetworkHandler.sendToServer(new RemoveLauncherFromTabletPacket(launchers.get(removed).pos()));
        launchers.remove(removed);
        routes.remove(removed);
        Set<Integer> shiftedChecked = new HashSet<>();
        for (int idx : checked) {
            if (idx < removed) {
                shiftedChecked.add(idx);
            } else if (idx > removed) {
                shiftedChecked.add(idx - 1);
            }
        }
        checked.clear();
        checked.addAll(shiftedChecked);
        Set<Integer> shiftedLaunched = new HashSet<>();
        for (int idx : launchedLocally) {
            if (idx < removed) {
                shiftedLaunched.add(idx);
            } else if (idx > removed) {
                shiftedLaunched.add(idx - 1);
            }
        }
        launchedLocally.clear();
        launchedLocally.addAll(shiftedLaunched);
        scrollOffset = Math.max(0, Math.min(scrollOffset, launchers.size() - MAX_VISIBLE_LAUNCHERS));
        selected = -1;
        rebuildLauncherButtons();
        updateButtonStates();
        updateCheckAllButton();
    }

    private void connectCamera(OpenUavTabletPacket.FlyingInfo drone) {
        if (!drone.camera()) {
            return;
        }
        NetworkHandler.sendToServer(new OrlanCameraViewPacket(drone.entityId(), true));
        OrlanCameraHandler.startCameraView(drone.entityId());
        this.onClose();
    }

    private void updateDroneRoute(OpenUavTabletPacket.FlyingInfo drone) {
        Entity entity = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getEntity(drone.entityId()) : null;
        BlockPos dronePos = entity != null ? entity.blockPosition() : new BlockPos(drone.x(), 64, drone.z());
        float yaw = entity != null ? entity.getYRot() : 0.0F;
        List<BlockPos> positions = new ArrayList<>();
        positions.add(dronePos);
        List<Float> yaws = new ArrayList<>();
        yaws.add(yaw);
        List<Boolean> drones = new ArrayList<>();
        drones.add(true);
        List<List<BlockPos>> droneRoutes = new ArrayList<>();
        droneRoutes.add(new ArrayList<>());
        final int entityId = drone.entityId();
        Minecraft.getInstance().setScreen(new OrlanRouteScreen(this, positions, yaws, drones, droneRoutes, updated -> {
            if (!updated.isEmpty() && !updated.get(0).isEmpty()) {
                NetworkHandler.sendToServer(new UpdateOrlanRoutePacket(entityId, updated.get(0)));
                statusMessage = Tr.s("gui.ata.recon_uav_tablet.route_updated");
            }
        }, entityId));
    }

    private void returnDrone(OpenUavTabletPacket.FlyingInfo drone) {
        NetworkHandler.sendToServer(new OrlanReturnPacket(drone.entityId()));
    }

    @Override
    public void tick() {
        super.tick();
        tickCounter++;
        for (Map.Entry<Integer, Integer> entry : localTimers.entrySet()) {
            if (entry.getValue() > 0) {
                entry.setValue(entry.getValue() - 1);
            }
        }
        if (tickCounter % 20 == 0) {
            rebuildDroneButtons();
            updateButtonStates();
        }
        if (tickCounter % REFRESH_TICKS == 0) {
            NetworkHandler.sendToServer(new RefreshUavTabletPacket());
        }
    }

    @Override
    public void renderScaled(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int guiLeft = left();
        int guiTop = top();
        int rightX = guiLeft + 205;

        g.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, BG_COLOR);
        g.fill(guiLeft, guiTop, guiLeft + GUI_WIDTH, guiTop + 2, BORDER_COLOR);
        g.fill(guiLeft, guiTop + GUI_HEIGHT - 2, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, BORDER_COLOR);
        g.fill(guiLeft, guiTop, guiLeft + 2, guiTop + GUI_HEIGHT, BORDER_COLOR);
        g.fill(guiLeft + GUI_WIDTH - 2, guiTop, guiLeft + GUI_WIDTH, guiTop + GUI_HEIGHT, BORDER_COLOR);

        g.drawCenteredString(this.font, Tr.s("gui.ata.uav_tablet.title"), guiLeft + GUI_WIDTH / 2, guiTop + 8, ACCENT_COLOR);
        g.drawString(this.font, Tr.s("gui.ata.uav_tablet.launchers"), guiLeft + 10, guiTop + 28, 0xAAAAAA, false);

        for (Button btn : launcherButtons) {
            btn.render(g, mouseX, mouseY, partialTick);
        }
        if (launchers.isEmpty()) {
            g.drawString(this.font, Tr.s("gui.ata.uav_tablet.no_launchers"), guiLeft + 14, guiTop + LIST_TOP + 6, 0x666666, false);
        }
        checkAllButton.render(g, mouseX, mouseY, partialTick);

        g.fill(guiLeft + 197, guiTop + 25, guiLeft + 199, guiTop + DIVIDER_Y - 6, BORDER_COLOR);

        if (selected >= 0 && selected < launchers.size()) {
            OpenUavTabletPacket.LauncherInfo info = launchers.get(selected);
            BlockPos pos = info.pos();
            int y = guiTop + 28;
            g.drawString(this.font, Tr.s("gui.ata.tablet.selected", selected + 1), rightX, y, 0xFFFF00, false);
            y += 12;
            g.drawString(this.font, Tr.s(info.heavy() ? "gui.ata.uav_tablet.heavy_launcher" : "gui.ata.uav_tablet.light_launcher")
                    + " §8[" + pos.getX() + ", " + pos.getY() + ", " + pos.getZ() + "]", rightX, y, 0x888888, false);
            y += 14;
            if (hasDrone(selected)) {
                String role = Tr.s(info.role() == UavLauncherEntity.ROLE_STRIKE ? "gui.ata.uav_tablet.strike" : "gui.ata.uav_tablet.recon");
                g.drawString(this.font, "§a● §f" + Tr.s(info.nameKey()) + " §7· " + role, rightX, y, 0xFFFFFF, false);
            } else {
                g.drawString(this.font, Tr.s("gui.ata.tablet.no_drone"), rightX, y, 0xFFFFFF, false);
            }
            y += 14;
            String routeText = hasRoute(selected) ? Tr.s("gui.ata.tablet.has_route", routes.get(selected).size()) : Tr.s("gui.ata.tablet.no_route");
            g.drawString(this.font, routeText, rightX, y, 0xFFFFFF, false);
            y += 14;
            if (!isPlayerInRange(selected)) {
                g.drawString(this.font, Tr.s("gui.ata.tablet.too_far_warn"), rightX, y, 0xFF4444, false);
                y += 11;
                g.drawString(this.font, Tr.s("gui.ata.tablet.max_range_hint", ClientRouteLimits.tabletRange()), rightX, y, 0x888888, false);
                y += 12;
            }
            if (!checked.isEmpty()) {
                int withDrone = 0;
                int withRoute = 0;
                for (int idx : checked) {
                    if (hasDrone(idx)) {
                        withDrone++;
                    }
                    if (hasRoute(idx)) {
                        withRoute++;
                    }
                }
                g.drawString(this.font, Tr.s("gui.ata.tablet.checked", checked.size()), rightX, y, 0xAAAAAA, false);
                y += 11;
                g.drawString(this.font, Tr.s("gui.ata.tablet.checked_stats", withDrone, withRoute), rightX, y, 0x777777, false);
            }
        } else {
            g.drawString(this.font, Tr.s("gui.ata.tablet.select_hint"), rightX, guiTop + 60, 0x666666, false);
            if (!checked.isEmpty()) {
                g.drawString(this.font, Tr.s("gui.ata.tablet.checked", checked.size()), rightX, guiTop + 80, 0xAAAAAA, false);
            }
        }

        routeButton.render(g, mouseX, mouseY, partialTick);
        removeButton.render(g, mouseX, mouseY, partialTick);
        launchButton.render(g, mouseX, mouseY, partialTick);
        launchAllButton.render(g, mouseX, mouseY, partialTick);

        g.fill(guiLeft + 5, guiTop + DIVIDER_Y, guiLeft + GUI_WIDTH - 5, guiTop + DIVIDER_Y + 2, BORDER_COLOR);
        g.drawString(this.font, Tr.s("gui.ata.uav_tablet.in_flight", flying.size()), guiLeft + 10, guiTop + DIVIDER_Y + 8, ACCENT_COLOR, false);
        if (flying.isEmpty()) {
            g.drawString(this.font, Tr.s("gui.ata.uav_tablet.none_in_flight"), guiLeft + 14, guiTop + DRONES_TOP + 6, 0x666666, false);
        }
        for (Button btn : droneButtons) {
            btn.render(g, mouseX, mouseY, partialTick);
        }
        for (Button btn : returnButtons) {
            String arrow = "↩";
            g.pose().pushPose();
            g.pose().translate(btn.getX() + btn.getWidth() / 2.0F, btn.getY() + btn.getHeight() / 2.0F, 0.0F);
            g.pose().scale(RETURN_ICON_SCALE, RETURN_ICON_SCALE, 1.0F);
            g.drawString(this.font, arrow, -this.font.width(arrow) / 2, -4, btn.active ? 0xFF5555 : 0x773333, false);
            g.pose().popPose();
        }

        if (!statusMessage.isEmpty()) {
            g.drawCenteredString(this.font, statusMessage, guiLeft + GUI_WIDTH / 2, guiTop + GUI_HEIGHT - 14, 0xFFFFFF);
        }

        super.renderScaled(g, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
