package com.synarsis.airtacticalarsenal.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.synarsis.airtacticalarsenal.client.ClientRouteLimits;
import com.synarsis.airtacticalarsenal.client.radar.RouteTerrainRenderer;
import com.synarsis.airtacticalarsenal.network.NetworkHandler;
import com.synarsis.airtacticalarsenal.network.RequestRouteMapPacket;
import com.synarsis.airtacticalarsenal.util.Tr;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.BiConsumer;

@OnlyIn(Dist.CLIENT)
public class MissileTargetMapScreen extends AtaScaledScreen {
    private static final int GUI_WIDTH = 440;
    private static final int GUI_HEIGHT = 340;
    private static final int MAP_X = 10;
    private static final int MAP_Y = 24;
    private static final int MAP_W = 420;
    private static final int MAP_H = 272;
    private static final double MIN_SCALE = 2.0;
    private static final int BG = 0xFF0D0D0D;
    private static final int ACCENT = 0xFFFF6600;
    private static final int MAX_RING = 0xFFFF3333;
    private static final int MIN_RING = 0x99FF9999;
    private static final int TARGET = 0xFFFFDD33;
    private static final int LAUNCHER = 0xFF55FF55;

    private final net.minecraft.client.gui.screens.Screen parent;
    private final BlockPos launcherPos;
    private final BiConsumer<Integer, Integer> onPick;
    private final int mapBpp;
    private BlockPos mapCenter;
    private RouteTerrainRenderer terrainRenderer;
    private int mapRequestTicks;
    private double viewCenterX;
    private double viewCenterZ;
    private double scale;
    private boolean panning;
    private boolean panMoved;
    private Integer targetX;
    private Integer targetZ;
    private Button doneButton;

    public MissileTargetMapScreen(net.minecraft.client.gui.screens.Screen parent, BlockPos launcherPos,
                                  Integer currentX, Integer currentZ, BiConsumer<Integer, Integer> onPick) {
        super(Component.translatable("gui.ata.missile_map.title"));
        this.parent = parent;
        this.launcherPos = launcherPos;
        this.onPick = onPick;
        this.targetX = currentX;
        this.targetZ = currentZ;
        this.viewCenterX = launcherPos.getX();
        this.viewCenterZ = launcherPos.getZ();
        this.mapBpp = ClientRouteLimits.routeBpp((int) Math.ceil(ClientRouteLimits.iskanderMax() * 1.05),
                launcherPos.getX(), launcherPos.getZ());
        if (RouteTerrainRenderer.hasCachedDataNear(launcherPos.getX(), launcherPos.getZ(), mapBpp)) {
            this.mapCenter = new BlockPos(RouteTerrainRenderer.getCachedCenterXNear(launcherPos.getX(), launcherPos.getZ(), mapBpp), 0,
                    RouteTerrainRenderer.getCachedCenterZNear(launcherPos.getX(), launcherPos.getZ(), mapBpp));
        } else {
            this.mapCenter = launcherPos;
        }
        this.scale = Math.min(maxScale(), Math.max(MIN_SCALE, ClientRouteLimits.iskanderMax() * 1.15 / (MAP_H / 2.0)));
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

    private double maxScale() {
        double half = Math.max(MAP_W, MAP_H) / 2.0;
        double reach = Math.max(80.0 * half, ClientRouteLimits.iskanderMax() * 1.3);
        reach = Math.min(reach, ClientRouteLimits.borderReach(launcherPos.getX(), launcherPos.getZ()) * 1.1);
        return Math.max(MIN_SCALE, reach / half);
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
        int y = top() + GUI_HEIGHT - 26;
        this.addRenderableWidget(Button.builder(Component.translatable("gui.ata.button.cancel_caps"), b -> onClose())
                .bounds(left() + GUI_WIDTH - 190, y, 85, 20).build());
        this.doneButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.ata.missile_map.use"), b -> done())
                .bounds(left() + GUI_WIDTH - 95, y, 85, 20).build());
        this.doneButton.active = validTarget();
    }

    private double distance() {
        if (targetX == null || targetZ == null) {
            return 0.0;
        }
        double dx = targetX - launcherPos.getX();
        double dz = targetZ - launcherPos.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private boolean validTarget() {
        if (targetX == null || targetZ == null) {
            return false;
        }
        double d = distance();
        return d >= ClientRouteLimits.iskanderMin() && d <= ClientRouteLimits.iskanderMax();
    }

    private void done() {
        if (validTarget()) {
            onPick.accept(targetX, targetZ);
        }
        onClose();
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
        if (terrainRenderer != null && !terrainRenderer.hasCacheData() && ++mapRequestTicks >= 10) {
            mapRequestTicks = 0;
            NetworkHandler.sendToServer(new RequestRouteMapPacket(mapCenter, mapBpp));
        }
    }

    private boolean onMap(double x, double y) {
        return x >= mapLeft() && x < mapLeft() + MAP_W && y >= mapTop() && y < mapTop() + MAP_H;
    }

    private double toScreenX(double worldX) {
        return mapLeft() + MAP_W / 2.0 + (worldX - viewCenterX) / scale;
    }

    private double toScreenY(double worldZ) {
        return mapTop() + MAP_H / 2.0 + (worldZ - viewCenterZ) / scale;
    }

    private double toWorldX(double screenX) {
        return viewCenterX + (screenX - (mapLeft() + MAP_W / 2.0)) * scale;
    }

    private double toWorldZ(double screenY) {
        return viewCenterZ + (screenY - (mapTop() + MAP_H / 2.0)) * scale;
    }

    @Override
    public boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        if (onMap(mouseX, mouseY) && (button == 0 || button == 1 || button == 2)) {
            panning = true;
            panMoved = false;
            return true;
        }
        return super.mouseClickedScaled(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (panning) {
            if (Math.abs(dragX) + Math.abs(dragY) > 0.5) {
                panMoved = true;
            }
            viewCenterX = MapBorder.clampX(viewCenterX - dragX * scale);
            viewCenterZ = MapBorder.clampZ(viewCenterZ - dragY * scale);
            return true;
        }
        return super.mouseDraggedScaled(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        if (panning) {
            panning = false;
            if (!panMoved && button == 0 && onMap(mouseX, mouseY)) {
                targetX = (int) Math.floor(MapBorder.clampX(toWorldX(mouseX)));
                targetZ = (int) Math.floor(MapBorder.clampZ(toWorldZ(mouseY)));
                if (doneButton != null) {
                    doneButton.active = validTarget();
                }
            }
            return true;
        }
        return super.mouseReleasedScaled(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        if (!onMap(mouseX, mouseY)) {
            return super.mouseScrolledScaled(mouseX, mouseY, delta);
        }
        double worldX = toWorldX(mouseX);
        double worldZ = toWorldZ(mouseY);
        double next = Math.max(MIN_SCALE, Math.min(maxScale(), scale * (delta > 0 ? 0.8 : 1.25)));
        viewCenterX = MapBorder.clampX(worldX - (mouseX - (mapLeft() + MAP_W / 2.0)) * next);
        viewCenterZ = MapBorder.clampZ(worldZ - (mouseY - (mapTop() + MAP_H / 2.0)) * next);
        scale = next;
        return true;
    }

    @Override
    public void renderScaled(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        int left = left();
        int top = top();
        g.fill(left, top, left + GUI_WIDTH, top + GUI_HEIGHT, BG);
        g.fill(left, top, left + GUI_WIDTH, top + 2, ACCENT);
        g.fill(left, top + GUI_HEIGHT - 2, left + GUI_WIDTH, top + GUI_HEIGHT, ACCENT);
        g.fill(left, top, left + 2, top + GUI_HEIGHT, ACCENT);
        g.fill(left + GUI_WIDTH - 2, top, left + GUI_WIDTH, top + GUI_HEIGHT, ACCENT);
        g.drawCenteredString(this.font, Tr.s("gui.ata.missile_map.header", launcherPos.getX(), launcherPos.getZ()),
                left + GUI_WIDTH / 2, top + 8, ACCENT);

        int mx = mapLeft();
        int my = mapTop();
        g.fill(mx, my, mx + MAP_W, my + MAP_H, 0xFF0A0A0A);
        renderTerrain(g, mx, my);
        MapBorder.shade(g, mx, my, mx + MAP_W, my + MAP_H, mx + MAP_W / 2, my + MAP_H / 2, viewCenterX, viewCenterZ, scale);

        double lx = launcherPos.getX() + 0.5;
        double lz = launcherPos.getZ() + 0.5;
        ring(g, lx, lz, ClientRouteLimits.iskanderMin(), MIN_RING, true);
        ring(g, lx, lz, ClientRouteLimits.iskanderMax(), MAX_RING, false);

        int sx = (int) Math.round(toScreenX(lx));
        int sy = (int) Math.round(toScreenY(lz));
        if (onMap(sx, sy)) {
            g.fill(sx - 3, sy - 3, sx + 4, sy + 4, 0xFF000000);
            g.fill(sx - 2, sy - 2, sx + 3, sy + 3, LAUNCHER);
        }
        if (targetX != null && targetZ != null) {
            int tx = (int) Math.round(toScreenX(targetX + 0.5));
            int ty = (int) Math.round(toScreenY(targetZ + 0.5));
            if (onMap(sx, sy) || onMap(tx, ty)) {
                line(g, sx, sy, tx, ty, validTarget() ? 0xAAFFDD33 : 0xAAFF5555);
            }
            if (onMap(tx, ty)) {
                int c = validTarget() ? TARGET : MAX_RING;
                g.fill(tx - 6, ty, tx - 1, ty + 1, c);
                g.fill(tx + 2, ty, tx + 7, ty + 1, c);
                g.fill(tx, ty - 6, tx + 1, ty - 1, c);
                g.fill(tx, ty + 2, tx + 1, ty + 7, c);
                com.synarsis.airtacticalarsenal.client.StrikeAimClient.outline(g, tx - 3, ty - 3, tx + 3, ty + 3, c);
            }
        }
        g.fill(mx, my, mx + MAP_W, my + 1, 0xFF444444);
        g.fill(mx, my + MAP_H - 1, mx + MAP_W, my + MAP_H, 0xFF444444);
        g.fill(mx, my, mx + 1, my + MAP_H, 0xFF444444);
        g.fill(mx + MAP_W - 1, my, mx + MAP_W, my + MAP_H, 0xFF444444);

        int infoY = top + GUI_HEIGHT - 38;
        int infoX = left + 12;
        if (targetX != null && targetZ != null) {
            double d = distance();
            boolean ok = validTarget();
            g.drawString(this.font, Tr.s("gui.ata.missile_map.target", targetX, targetZ), infoX, infoY, 0xFFFFFFFF, false);
            g.drawString(this.font, Tr.s("gui.ata.target_settings.dist", d), infoX, infoY + 12, ok ? 0xFF55FF55 : 0xFFFF5555, false);
            g.drawString(this.font, Tr.s("gui.ata.target_settings.cep", (double) Math.round(10.0 + d / 100.0)), infoX + 110, infoY + 12, 0xFFAAAAAA, false);
        } else {
            g.drawString(this.font, Tr.s("gui.ata.missile_map.hint"), infoX, infoY, 0xFFAAAAAA, false);
        }
        g.drawString(this.font, Tr.s("gui.ata.target_settings.range", ClientRouteLimits.iskanderMin(), ClientRouteLimits.iskanderMax()),
                infoX, infoY + 24, 0xFFFF7777, false);
        if (onMap(mouseX, mouseY)) {
            String c = (int) Math.floor(toWorldX(mouseX)) + ", " + (int) Math.floor(toWorldZ(mouseY));
            g.drawString(this.font, c, mx + MAP_W - 4 - this.font.width(c), my + MAP_H - 12, 0xCCFFFFFF, true);
        }
        super.renderScaled(g, mouseX, mouseY, partialTick);
    }

    private void ring(GuiGraphics g, double cx, double cz, double radius, int color, boolean dashed) {
        double pixels = radius / scale;
        int steps = (int) Math.max(64, Math.min(4000, pixels * 2.0 * Math.PI));
        for (int i = 0; i < steps; i++) {
            if (dashed && (i / 4) % 2 == 1) {
                continue;
            }
            double a = i * 2.0 * Math.PI / steps;
            int x = (int) Math.round(toScreenX(cx + Math.cos(a) * radius));
            int y = (int) Math.round(toScreenY(cz + Math.sin(a) * radius));
            if (onMap(x, y)) {
                g.fill(x, y, x + 1, y + 1, color);
            }
        }
    }

    private void line(GuiGraphics g, int x0, int y0, int x1, int y1, int color) {
        int steps = Math.max(Math.abs(x1 - x0), Math.abs(y1 - y0));
        if (steps > 6000) {
            return;
        }
        for (int i = 0; i <= steps; i += 2) {
            int x = steps == 0 ? x0 : x0 + (x1 - x0) * i / steps;
            int y = steps == 0 ? y0 : y0 + (y1 - y0) * i / steps;
            if (onMap(x, y)) {
                g.fill(x, y, x + 1, y + 1, color);
            }
        }
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
