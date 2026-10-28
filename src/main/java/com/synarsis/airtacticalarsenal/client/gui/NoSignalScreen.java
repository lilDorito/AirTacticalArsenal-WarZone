package com.synarsis.airtacticalarsenal.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class NoSignalScreen extends Screen {
    private static final int RED = 0xFFE02020;
    private final long opened = System.nanoTime();
    private static final int BORDER = 32;
    private final RandomSource random = RandomSource.create();
    private final int frameColor;

    public NoSignalScreen(int frameColor) {
        super(Component.translatable("hud.ata.camera.no_signal"));
        this.frameColor = frameColor;
    }

    private boolean hidGui;

    @Override
    protected void init() {
        super.init();
        if (this.minecraft != null && !this.minecraft.options.hideGui) {
            this.minecraft.options.hideGui = true;
            this.hidGui = true;
        }
    }

    @Override
    public void removed() {
        if (this.hidGui && this.minecraft != null) {
            this.minecraft.options.hideGui = false;
            this.hidGui = false;
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_LEFT_ALT || keyCode == GLFW.GLFW_KEY_RIGHT_ALT) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int sw = this.width;
        int sh = this.height;
        double t = (System.nanoTime() - this.opened) / 1.0E9;
        g.fill(0, 0, sw, BORDER, 0xDD000000);
        g.fill(0, sh - BORDER, sw, sh, 0xDD000000);
        g.fill(0, BORDER, BORDER, sh - BORDER, 0xDD000000);
        g.fill(sw - BORDER, BORDER, sw, sh - BORDER, 0xDD000000);

        g.pose().pushPose();
        g.pose().translate(BORDER, BORDER, 0.0F);
        g.enableScissor(BORDER, BORDER, sw - BORDER, sh - BORDER);
        int w = sw - BORDER * 2;
        int h = sh - BORDER * 2;
        g.fill(0, 0, w, h, 0xFF000000);

        for (int y = 0; y < h; y += 3) {
            g.fill(0, y, w, y + 1, 0x0CFFFFFF);
        }

        int dots = w * h / 45;
        for (int i = 0; i < dots; i++) {
            int x = this.random.nextInt(w);
            int y = this.random.nextInt(h);
            int v = 40 + this.random.nextInt(140);
            int a = 30 + this.random.nextInt(70);
            g.fill(x, y, x + 1 + this.random.nextInt(2), y + 1, (a << 24) | (v << 16) | (v << 8) | v);
        }

        int bands = 2 + this.random.nextInt(4);
        for (int i = 0; i < bands; i++) {
            int y = this.random.nextInt(h);
            int bh = 1 + this.random.nextInt(3);
            int v = 90 + this.random.nextInt(120);
            int a = 25 + this.random.nextInt(50);
            g.fill(0, y, w, y + bh, (a << 24) | (v << 16) | (v << 8) | v);
        }

        int roll = (int) ((t * 45.0) % (h + 40)) - 40;
        g.fill(0, roll, w, roll + 22, 0x14FFFFFF);
        g.fill(0, roll + 8, w, roll + 12, 0x10FFFFFF);

        Component text = Component.translatable("hud.ata.camera.no_signal").withStyle(ChatFormatting.BOLD);
        float scale = 3.0F;
        int textW = this.font.width(text);
        int jitter = (this.random.nextInt(14) == 0) ? this.random.nextInt(7) - 3 : 0;
        g.pose().pushPose();
        g.pose().translate(w / 2.0F + jitter, h / 2.0F - 12.0F, 0.0F);
        g.pose().scale(scale, scale, 1.0F);
        g.drawString(this.font, text, -textW / 2 + 1, 0, 0x6620E0E0, false);
        g.drawString(this.font, text, -textW / 2, 0, RED, false);
        g.pose().popPose();
        g.disableScissor();
        g.pose().popPose();

        g.fill(BORDER, BORDER, sw - BORDER, BORDER + 1, this.frameColor);
        g.fill(BORDER, sh - BORDER - 1, sw - BORDER, sh - BORDER, this.frameColor);
        g.fill(BORDER, BORDER, BORDER + 1, sh - BORDER, this.frameColor);
        g.fill(sw - BORDER - 1, BORDER, sw - BORDER, sh - BORDER, this.frameColor);

        Component hint = Component.translatable("hud.ata.camera.no_signal_hint");
        g.drawString(this.font, hint, sw / 2 - this.font.width(hint) / 2, sh - BORDER + 12, 0xFF9A9A9A, false);
    }
}
