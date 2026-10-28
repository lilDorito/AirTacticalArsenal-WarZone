package com.synarsis.airtacticalarsenal.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.synarsis.airtacticalarsenal.api.DroneVariants;
import com.synarsis.airtacticalarsenal.client.ClientDroneNames;
import com.synarsis.airtacticalarsenal.client.OrlanCameraHandler;
import com.synarsis.airtacticalarsenal.client.radar.RouteTerrainRenderer;
import com.synarsis.airtacticalarsenal.entity.OrlanEntity;
import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.item.ModItems;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.OpenUavTabletPacket;
import com.synarsis.airtacticalarsenal.network.OrlanCameraViewPacket;
import com.synarsis.airtacticalarsenal.network.OrlanReturnPacket;
import com.synarsis.airtacticalarsenal.network.RefreshUavTabletPacket;
import com.synarsis.airtacticalarsenal.network.RequestRouteMapPacket;
import com.synarsis.airtacticalarsenal.util.Tr;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class StrategicMapScreen extends AtaScaledScreen {
    private static final int GUI_WIDTH = 520;
    private static final int GUI_HEIGHT = 440;
    private static final int MAP_X = 10;
    private static final int MAP_Y = 24;
    private static final int MAP_W = 500;
    private static final int MAP_H = 380;
    private static final int BG_COLOR = 0xE0101010;
    private static final int BORDER_COLOR = 0xFF2E7D32;
    private static final int ACCENT_COLOR = 0xFF66BB6A;
    private static final int STRIKE_COLOR = 0xFFFF5544;
    private static final int RECON_COLOR = 0xFF55CCFF;
    private static final int INTERCEPTOR_COLOR = 0xFFCC66FF;

    private static int colorOf(OpenUavTabletPacket.FlyingInfo info) {
        if (info.recon()) {
            return RECON_COLOR;
        }
        return com.synarsis.airtacticalarsenal.api.DroneVariants.interceptor(info.kind()) ? INTERCEPTOR_COLOR : STRIKE_COLOR;
    }
    private static final int PLAYER_COLOR = 0xFF4488FF;
    private static final int LAUNCHER_COLOR = 0xFF66BB6A;
    private static final double MIN_SCALE = 2.0;
    private static final int GROUP_PIXELS = 14;
    private static final int ROW_H = 18;
    private static final int ROW_W_MIN = 90;
    private static final int ROW_W_MAX = 260;
    private static final int REFRESH_TICKS = 40;

    private final UavTabletScreen parent;
    private final List<OpenUavTabletPacket.LauncherInfo> launchers = new ArrayList<>();
    private final List<OpenUavTabletPacket.FlyingInfo> flying = new ArrayList<>();
    private final List<Group> groups = new ArrayList<>();

    private double viewCenterX;
    private double viewCenterZ;
    private double scale = 20.0;
    private boolean panning;
    private boolean panMoved;
    private Group open;
    private int openX;
    private int openY;
    private int tickCounter;
    private int mapRequestTicks;
    private RouteTerrainRenderer terrainRenderer;
    private final BlockPos mapCenter;
    private final int mapBpp;

    private record Group(int x, int y, List<OpenUavTabletPacket.FlyingInfo> drones) {
    }

    public StrategicMapScreen(UavTabletScreen parent, List<OpenUavTabletPacket.LauncherInfo> launchers,
                              List<OpenUavTabletPacket.FlyingInfo> flying) {
        super(Component.literal(""));
        this.parent = parent;
        update(launchers, flying);
        Minecraft mc = Minecraft.getInstance();
        BlockPos center = mc.player != null ? mc.player.blockPosition() : BlockPos.ZERO;
        this.viewCenterX = center.getX();
        this.viewCenterZ = center.getZ();
        this.mapBpp = com.synarsis.airtacticalarsenal.client.ClientRouteLimits.routeBpp(
                com.synarsis.airtacticalarsenal.client.ClientRouteLimits.maxReach(), center.getX(), center.getZ());
        if (RouteTerrainRenderer.hasCachedDataNear(center.getX(), center.getZ(), mapBpp)) {
            this.mapCenter = new BlockPos(RouteTerrainRenderer.getCachedCenterXNear(center.getX(), center.getZ(), mapBpp), 0,
                    RouteTerrainRenderer.getCachedCenterZNear(center.getX(), center.getZ(), mapBpp));
        } else {
            this.mapCenter = center;
        }
        this.scale = Math.min(scale, maxScale());
    }

    public void update(List<OpenUavTabletPacket.LauncherInfo> newLaunchers, List<OpenUavTabletPacket.FlyingInfo> newFlying) {
        launchers.clear();
        launchers.addAll(newLaunchers);
        flying.clear();
        flying.addAll(newFlying);
        if (open != null) {
            List<OpenUavTabletPacket.FlyingInfo> kept = new ArrayList<>();
            for (OpenUavTabletPacket.FlyingInfo old : open.drones()) {
                for (OpenUavTabletPacket.FlyingInfo now : flying) {
                    if (now.entityId() == old.entityId()) {
                        kept.add(now);
                    }
                }
            }
            open = kept.isEmpty() ? null : new Group(open.x(), open.y(), kept);
        }
        if (parent != null) {
            parent.update(newLaunchers, newFlying);
        }
    }

    @Override
    protected int designWidth() {
        return GUI_WIDTH;
    }

    @Override
    protected int designHeight() {
        return GUI_HEIGHT;
    }

    private int left() {
        return (this.width - GUI_WIDTH) / 2;
    }

    private int top() {
        return (this.height - GUI_HEIGHT) / 2;
    }

    private int mapLeft() {
        return left() + MAP_X;
    }

    private int mapTop() {
        return top() + MAP_Y;
    }

    @Override
    protected void init() {
        super.init();
        if (terrainRenderer == null) {
            terrainRenderer = new RouteTerrainRenderer(mapCenter.getX(), mapCenter.getZ(), mapBpp);
            if (!terrainRenderer.hasCacheData()) {
                NetworkHandler.sendToServer(new RequestRouteMapPacket(mapCenter, mapBpp));
            }
        }
        int y = top() + GUI_HEIGHT - 28;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ata.strategic.back"), b -> onClose())
                .bounds(left() + 10, y, 90, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ata.strategic.center"), b -> centerOnPlayer())
                .bounds(left() + 105, y, 90, 20).build());
    }

    private void centerOnPlayer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            viewCenterX = MapBorder.clampX(mc.player.getX());
            viewCenterZ = MapBorder.clampZ(mc.player.getZ());
        }
        open = null;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    public void removed() {
        super.removed();
        if (terrainRenderer != null) {
            terrainRenderer.close();
            terrainRenderer = null;
        }
    }

    @Override
    public void tick() {
        super.tick();
        tickCounter++;
        if (tickCounter % REFRESH_TICKS == 0) {
            NetworkHandler.sendToServer(new RefreshUavTabletPacket());
        }
        if (terrainRenderer != null && !terrainRenderer.hasCacheData()) {
            if (++mapRequestTicks >= 10) {
                mapRequestTicks = 0;
                NetworkHandler.sendToServer(new RequestRouteMapPacket(mapCenter, mapBpp));
            }
        }
    }

    private double maxScale() {
        double half = Math.max(MAP_W, MAP_H) / 2.0;
        double reach = Math.max(80.0 * half, com.synarsis.airtacticalarsenal.client.ClientRouteLimits.maxReach() * 1.1);
        reach = Math.min(reach, com.synarsis.airtacticalarsenal.client.ClientRouteLimits.borderReach(mapCenter.getX(), mapCenter.getZ()) * 1.1);
        return Math.max(MIN_SCALE, reach / half);
    }

    private int toScreenX(double worldX) {
        return mapLeft() + MAP_W / 2 + (int) Math.round((worldX - viewCenterX) / scale);
    }

    private int toScreenY(double worldZ) {
        return mapTop() + MAP_H / 2 + (int) Math.round((worldZ - viewCenterZ) / scale);
    }

    private boolean onMap(double x, double y) {
        return x >= mapLeft() && x < mapLeft() + MAP_W && y >= mapTop() && y < mapTop() + MAP_H;
    }

    private Vec3 dronePos(OpenUavTabletPacket.FlyingInfo info) {
        Minecraft mc = Minecraft.getInstance();
        Entity entity = mc.level != null ? mc.level.getEntity(info.entityId()) : null;
        if (entity != null && entity.isAlive()) {
            return entity.position();
        }
        return new Vec3(info.x(), 0, info.z());
    }

    private void buildGroups() {
        groups.clear();
        for (OpenUavTabletPacket.FlyingInfo info : flying) {
            Vec3 pos = dronePos(info);
            int x = toScreenX(pos.x);
            int y = toScreenY(pos.z);
            Group hit = null;
            for (Group group : groups) {
                if (Math.abs(group.x() - x) <= GROUP_PIXELS && Math.abs(group.y() - y) <= GROUP_PIXELS) {
                    hit = group;
                    break;
                }
            }
            if (hit != null) {
                hit.drones().add(info);
            } else {
                List<OpenUavTabletPacket.FlyingInfo> list = new ArrayList<>();
                list.add(info);
                groups.add(new Group(x, y, list));
            }
        }
    }

    private ItemStack iconOf(OpenUavTabletPacket.FlyingInfo info) {
        ItemStack stack = DroneVariants.stackOf(info.kind());
        if (!stack.isEmpty()) {
            return stack;
        }
        return new ItemStack(info.recon() ? ModItems.ORLAN.get() : ModItems.SHAHED.get());
    }

    private String nameOf(OpenUavTabletPacket.FlyingInfo info) {
        return ClientDroneNames.name(info.entityId(), info.recon() ? "drone.ata.orlan" : "drone.ata.shahed");
    }

    private static boolean landed(OpenUavTabletPacket.FlyingInfo info) {
        return info.recon() && info.phase() == OrlanEntity.FlightPhase.LANDED.ordinal();
    }

    private static boolean canReturn(OpenUavTabletPacket.FlyingInfo info) {
        return info.recon() && info.entityId() >= 0 && !landed(info);
    }

    private boolean canRoute(OpenUavTabletPacket.FlyingInfo info) {
        if (parent == null || !UavTabletScreen.canReroute(info) || landed(info)) {
            return false;
        }
        if (info.recon()) {
            int phase = info.phase();
            return phase == OrlanEntity.FlightPhase.CRUISING.ordinal() || phase == OrlanEntity.FlightPhase.PATROLLING.ordinal();
        }
        return true;
    }

    private int routeSlot(OpenUavTabletPacket.FlyingInfo info) {
        return canReturn(info) ? rowWidth() - 44 : rowWidth() - 22;
    }

    private int rowWidth() {
        if (open == null) {
            return ROW_W_MIN;
        }
        int widest = ROW_W_MIN;
        for (OpenUavTabletPacket.FlyingInfo info : open.drones()) {
            int w = 20 + this.font.width(phaseOf(info) + " §f" + nameOf(info)) + 6;
            if (info.camera()) {
                w += 6 + this.font.width(Tr.s("gui.ata.uav_tablet.camera"));
            }
            if (canRoute(info)) {
                w += 22;
            }
            if (canReturn(info)) {
                w += 22;
            }
            widest = Math.max(widest, w);
        }
        return Math.min(ROW_W_MAX, widest);
    }

    private static String phaseOf(OpenUavTabletPacket.FlyingInfo info) {
        if (info.recon()) {
            OrlanEntity.FlightPhase[] phases = OrlanEntity.FlightPhase.values();
            if (info.phase() < 0 || info.phase() >= phases.length) {
                return "";
            }
            return switch (phases[info.phase()]) {
                case LAUNCHING, CLIMBING -> "§d▲";
                case CRUISING -> "§6→";
                case PATROLLING -> "§a◎";
                case RETURNING -> "§c↩";
                case LANDING -> "§e▼";
                case LANDED -> "§7■";
            };
        }
        ShahedEntity.FlightPhase[] phases = ShahedEntity.FlightPhase.values();
        if (info.phase() < 0 || info.phase() >= phases.length) {
            return "";
        }
        return switch (phases[info.phase()]) {
            case WARMUP, LAUNCHING, CLIMBING -> "§d▲";
            case CRUISING -> "§6→";
            case DIVING -> "§c▼";
        };
    }

    @Override
    public boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (open != null) {
            int index = rowAt(mouseX, mouseY);
            if (index >= 0) {
                OpenUavTabletPacket.FlyingInfo info = open.drones().get(index);
                if (canReturn(info) && mouseX >= openX + rowWidth() - 22) {
                    NetworkHandler.sendToServer(new OrlanReturnPacket(info.entityId()));
                    return true;
                }
                int slot = openX + routeSlot(info);
                if (canRoute(info) && mouseX >= slot && mouseX < slot + 22) {
                    parent.updateDroneRoute(info);
                    return true;
                }
                if (info.camera()) {
                    connectCamera(info);
                }
                return true;
            }
            open = null;
        }
        if (button == 0 && onMap(mouseX, mouseY)) {
            Group group = groupAt(mouseX, mouseY);
            if (group != null) {
                open = group;
                openX = Math.min(group.x() + 10, mapLeft() + MAP_W - rowWidth());
                openY = Math.min(group.y() + 10, mapTop() + MAP_H - ROW_H * group.drones().size());
                return true;
            }
            panning = true;
            panMoved = false;
            return true;
        }
        return super.mouseClickedScaled(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        if (button == 0 && panning) {
            panning = false;
            return true;
        }
        return super.mouseReleasedScaled(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panning) {
            viewCenterX = MapBorder.clampX(viewCenterX - dragX * scale);
            viewCenterZ = MapBorder.clampZ(viewCenterZ - dragY * scale);
            panMoved = true;
            open = null;
            return true;
        }
        return super.mouseDraggedScaled(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        if (!onMap(mouseX, mouseY)) {
            return super.mouseScrolledScaled(mouseX, mouseY, delta);
        }
        double worldX = viewCenterX + (mouseX - (mapLeft() + MAP_W / 2.0)) * scale;
        double worldZ = viewCenterZ + (mouseY - (mapTop() + MAP_H / 2.0)) * scale;
        double next = Math.max(MIN_SCALE, Math.min(maxScale(), scale * (delta > 0 ? 0.8 : 1.25)));
        viewCenterX = MapBorder.clampX(worldX - (mouseX - (mapLeft() + MAP_W / 2.0)) * next);
        viewCenterZ = MapBorder.clampZ(worldZ - (mouseY - (mapTop() + MAP_H / 2.0)) * next);
        scale = next;
        open = null;
        return true;
    }

    private Group groupAt(double mouseX, double mouseY) {
        for (Group group : groups) {
            if (Math.abs(group.x() - mouseX) <= 9 && Math.abs(group.y() - mouseY) <= 9) {
                return group;
            }
        }
        return null;
    }

    private int rowAt(double mouseX, double mouseY) {
        if (open == null || mouseX < openX || mouseX >= openX + rowWidth()) {
            return -1;
        }
        int index = (int) Math.floor((mouseY - openY) / ROW_H);
        return mouseY >= openY && index < open.drones().size() ? index : -1;
    }

    private void connectCamera(OpenUavTabletPacket.FlyingInfo info) {
        NetworkHandler.sendToServer(new com.synarsis.airtacticalarsenal.network.CameraRenderDistancePacket(
                Minecraft.getInstance().options.getEffectiveRenderDistance()));
        NetworkHandler.sendToServer(new OrlanCameraViewPacket(info.entityId(), true));
        OrlanCameraHandler.startCameraView(info.entityId());
        Minecraft.getInstance().setScreen(null);
    }

    @Override
    public void renderScaled(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int left = left();
        int top = top();
        g.fill(left, top, left + GUI_WIDTH, top + GUI_HEIGHT, BG_COLOR);
        g.fill(left, top, left + GUI_WIDTH, top + 2, BORDER_COLOR);
        g.fill(left, top + GUI_HEIGHT - 2, left + GUI_WIDTH, top + GUI_HEIGHT, BORDER_COLOR);
        g.fill(left, top, left + 2, top + GUI_HEIGHT, BORDER_COLOR);
        g.fill(left + GUI_WIDTH - 2, top, left + GUI_WIDTH, top + GUI_HEIGHT, BORDER_COLOR);
        g.drawCenteredString(this.font, Tr.s("gui.ata.strategic.title", flying.size()), left + GUI_WIDTH / 2, top + 8, ACCENT_COLOR);

        int mx = mapLeft();
        int my = mapTop();
        g.fill(mx, my, mx + MAP_W, my + MAP_H, 0xFF0A0A0A);
        renderTerrain(g, mx, my);
        MapBorder.shade(g, mx, my, mx + MAP_W, my + MAP_H, mx + MAP_W / 2, my + MAP_H / 2, viewCenterX, viewCenterZ, scale);

        for (OpenUavTabletPacket.LauncherInfo launcher : launchers) {
            int x = toScreenX(launcher.pos().getX() + 0.5);
            int y = toScreenY(launcher.pos().getZ() + 0.5);
            if (onMap(x, y)) {
                g.fill(x - 2, y - 2, x + 3, y + 3, LAUNCHER_COLOR);
            }
        }

        for (OpenUavTabletPacket.FlyingInfo info : flying) {
            Vec3 pos = dronePos(info);
            int x = toScreenX(pos.x);
            int y = toScreenY(pos.z);
            int tx = toScreenX(info.targetX() + 0.5);
            int ty = toScreenY(info.targetZ() + 0.5);
            int color = colorOf(info);
            if (landed(info)) {
                continue;
            }
            dashed(g, x, y, tx, ty, (color & 0x00FFFFFF) | 0x88000000);
            if (onMap(tx, ty)) {
                g.fill(tx - 2, ty, tx + 3, ty + 1, color);
                g.fill(tx, ty - 2, tx + 1, ty + 3, color);
            }
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            int px = toScreenX(mc.player.getX());
            int py = toScreenY(mc.player.getZ());
            if (onMap(px, py)) {
                g.fill(px - 3, py - 3, px + 4, py + 4, 0xFF000000);
                g.fill(px - 2, py - 2, px + 3, py + 3, PLAYER_COLOR);
            }
        }

        buildGroups();
        Group hover = open == null ? groupAt(mouseX, mouseY) : null;
        for (Group group : groups) {
            if (!onMap(group.x(), group.y())) {
                continue;
            }
            OpenUavTabletPacket.FlyingInfo first = group.drones().get(0);
            int color = colorOf(first);
            int x = group.x();
            int y = group.y();
            g.fill(x - 9, y - 9, x + 9, y + 9, group == hover || group.drones().equals(open != null ? open.drones() : null) ? 0xCC333333 : 0x99000000);
            g.fill(x - 9, y - 9, x + 9, y - 8, color);
            g.fill(x - 9, y + 8, x + 9, y + 9, color);
            g.fill(x - 9, y - 9, x - 8, y + 9, color);
            g.fill(x + 8, y - 9, x + 9, y + 9, color);
            g.renderItem(iconOf(first), x - 8, y - 8);
            if (group.drones().size() > 1) {
                String count = "×" + group.drones().size();
                g.pose().pushPose();
                g.pose().translate(0, 0, 200);
                g.drawString(this.font, count, x + 10, y + 2, 0xFFFFFFFF, true);
                g.pose().popPose();
            }
        }

        g.fill(mx - 1, my - 1, mx + MAP_W + 1, my, 0xFF333333);
        g.fill(mx - 1, my + MAP_H, mx + MAP_W + 1, my + MAP_H + 1, 0xFF333333);
        g.fill(mx - 1, my, mx, my + MAP_H, 0xFF333333);
        g.fill(mx + MAP_W, my, mx + MAP_W + 1, my + MAP_H, 0xFF333333);
        g.drawString(this.font, "N", mx + MAP_W / 2 - 2, my + 2, 0x88FFFFFF, false);
        String scaleText = Tr.s("gui.ata.route.scale", Math.round(scale));
        g.fill(mx + 1, my + MAP_H - 11, mx + 4 + this.font.width(scaleText), my + MAP_H - 1, 0xAA000000);
        g.drawString(this.font, scaleText, mx + 3, my + MAP_H - 10, 0xAAAAAA, false);

        if (flying.isEmpty()) {
            String none = Tr.s("gui.ata.strategic.none");
            g.drawCenteredString(this.font, none, mx + MAP_W / 2, my + MAP_H / 2 - 4, 0xFF888888);
        }

        if (hover != null) {
            List<Component> lines = new ArrayList<>();
            for (OpenUavTabletPacket.FlyingInfo info : hover.drones()) {
                lines.add(Component.literal(phaseOf(info) + " §f" + nameOf(info)));
            }
            g.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }

        if (open != null) {
            renderList(g, mouseX, mouseY);
        }

        g.drawString(this.font, Tr.s("gui.ata.strategic.hint"), left + 205, top + GUI_HEIGHT - 22, 0xFF777777, false);
        super.renderScaled(g, mouseX, mouseY, partialTick);
    }

    private void renderList(GuiGraphics g, int mouseX, int mouseY) {
        g.pose().pushPose();
        g.pose().translate(0, 0, 300);
        int rows = open.drones().size();
        g.fill(openX - 1, openY - 1, openX + rowWidth() + 1, openY + rows * ROW_H + 1, 0xFF2E7D32);
        for (int i = 0; i < rows; i++) {
            OpenUavTabletPacket.FlyingInfo info = open.drones().get(i);
            int y = openY + i * ROW_H;
            boolean hovered = rowAt(mouseX, mouseY) == i;
            g.fill(openX, y, openX + rowWidth(), y + ROW_H, hovered ? 0xF0303A30 : 0xF0141414);
            g.renderItem(iconOf(info), openX + 1, y + 1);
            String label = phaseOf(info) + " §f" + nameOf(info);
            g.drawString(this.font, label, openX + 20, y + 5, 0xFFFFFFFF, false);
            if (info.camera()) {
                String cam = Tr.s("gui.ata.uav_tablet.camera");
                int right = canRoute(info) ? openX + routeSlot(info) - 4 : canReturn(info) ? openX + rowWidth() - 26 : openX + rowWidth() - 4;
                g.drawString(this.font, "§a" + cam, right - this.font.width(cam), y + 5, 0xFF55FF55, false);
            }
            if (canRoute(info)) {
                int slot = openX + routeSlot(info);
                boolean over = hovered && mouseX >= slot && mouseX < slot + 22;
                g.pose().pushPose();
                g.pose().translate(slot + 11, y + ROW_H / 2.0F, 0.0F);
                g.pose().scale(2.0F, 2.0F, 1.0F);
                g.drawString(this.font, "→", -this.font.width("→") / 2, -4, over ? 0xFFFFEE88 : 0xFFFFAA00, false);
                g.pose().popPose();
            }
            if (canReturn(info)) {
                g.pose().pushPose();
                g.pose().translate(openX + rowWidth() - 11, y + ROW_H / 2.0F, 0.0F);
                g.pose().scale(2.0F, 2.0F, 1.0F);
                g.drawString(this.font, "↩", -this.font.width("↩") / 2, -4, 0xFFFF5555, false);
                g.pose().popPose();
            }
        }
        g.pose().popPose();
    }

    private void dashed(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        double[] seg = clip(x0, y0, x1, y1);
        if (seg == null) {
            return;
        }
        double dx = seg[2] - seg[0];
        double dy = seg[3] - seg[1];
        double length = Math.sqrt(dx * dx + dy * dy);
        if (length < 1.0) {
            return;
        }
        int steps = (int) length;
        for (int i = 0; i <= steps; i++) {
            if ((i / 4) % 2 == 1) {
                continue;
            }
            int x = (int) Math.round(seg[0] + dx * i / length);
            int y = (int) Math.round(seg[1] + dy * i / length);
            g.fill(x, y, x + 1, y + 1, color);
        }
    }

    private double[] clip(double x0, double y0, double x1, double y1) {
        double minX = mapLeft();
        double minY = mapTop();
        double maxX = mapLeft() + MAP_W - 1;
        double maxY = mapTop() + MAP_H - 1;
        double t0 = 0.0;
        double t1 = 1.0;
        double dx = x1 - x0;
        double dy = y1 - y0;
        double[] p = {-dx, dx, -dy, dy};
        double[] q = {x0 - minX, maxX - x0, y0 - minY, maxY - y0};
        for (int i = 0; i < 4; i++) {
            if (p[i] == 0) {
                if (q[i] < 0) {
                    return null;
                }
                continue;
            }
            double t = q[i] / p[i];
            if (p[i] < 0) {
                t0 = Math.max(t0, t);
            } else {
                t1 = Math.min(t1, t);
            }
            if (t0 > t1) {
                return null;
            }
        }
        return new double[]{x0 + t0 * dx, y0 + t0 * dy, x0 + t1 * dx, y0 + t1 * dy};
    }

    private void renderTerrain(GuiGraphics g, int mapX, int mapY) {
        if (terrainRenderer == null || !terrainRenderer.isReady()) {
            return;
        }
        if (!terrainRenderer.hasCacheData()) {
            String text = terrainRenderer.isQueued()
                    ? Tr.s("gui.ata.map.queued", terrainRenderer.getQueuePosition())
                    : terrainRenderer.isBuilding()
                    ? Tr.s("gui.ata.map.scanning", (int) (terrainRenderer.getBuildProgress() * 100))
                    : Tr.s("gui.ata.map.loading");
            int tw = this.font.width(text);
            int tx = mapX + (MAP_W - tw) / 2;
            int ty = mapY + MAP_H - 16;
            g.fill(tx - 4, ty - 3, tx + tw + 4, ty + 11, 0xB0000000);
            g.drawString(this.font, text, tx, ty, 0xFF40FF80, false);
            return;
        }
        int texSize = terrainRenderer.getTextureSize();
        double bpp = terrainRenderer.getBpp();
        double halfW = (MAP_W / 2.0) * scale;
        double halfH = (MAP_H / 2.0) * scale;
        double panX = viewCenterX - terrainRenderer.getCenterX();
        double panZ = viewCenterZ - terrainRenderer.getCenterZ();
        float srcLeft = (float) (texSize / 2.0 + (panX - halfW) / bpp);
        float srcTop = (float) (texSize / 2.0 + (panZ - halfH) / bpp);
        float srcRight = (float) (texSize / 2.0 + (panX + halfW) / bpp);
        float srcBottom = (float) (texSize / 2.0 + (panZ + halfH) / bpp);
        float fullW = srcRight - srcLeft;
        float fullH = srcBottom - srcTop;
        if (fullW <= 0 || fullH <= 0) {
            return;
        }
        int dstLeft = mapX;
        int dstTop = mapY;
        int dstRight = mapX + MAP_W;
        int dstBottom = mapY + MAP_H;
        if (srcLeft < 0) {
            dstLeft += (int) (-srcLeft / fullW * MAP_W);
            srcLeft = 0;
        }
        if (srcTop < 0) {
            dstTop += (int) (-srcTop / fullH * MAP_H);
            srcTop = 0;
        }
        if (srcRight > texSize) {
            dstRight -= (int) ((srcRight - texSize) / fullW * MAP_W);
            srcRight = texSize;
        }
        if (srcBottom > texSize) {
            dstBottom -= (int) ((srcBottom - texSize) / fullH * MAP_H);
            srcBottom = texSize;
        }
        int dstW = dstRight - dstLeft;
        int dstH = dstBottom - dstTop;
        int srcW = Math.max(1, (int) (srcRight - srcLeft));
        int srcH = Math.max(1, (int) (srcBottom - srcTop));
        if (dstW > 0 && dstH > 0) {
            RenderSystem.setShaderTexture(0, terrainRenderer.getTextureLocation());
            g.blit(terrainRenderer.getTextureLocation(), dstLeft, dstTop, dstW, dstH,
                    srcLeft, srcTop, srcW, srcH, texSize, texSize);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
