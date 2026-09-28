package com.synarsis.airtacticalarsenal.client.gui;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AtaScaledScreen extends Screen {

    private static final int MARGIN = 8;
    private static final float MIN_SCALE = 0.25f;

    protected float uiScale = 1.0f;

    protected AtaScaledScreen(Component title) {
        super(title);
    }

    protected abstract int designWidth();

    protected abstract int designHeight();

    @Override
    protected void init() {
        Window window = Minecraft.getInstance().getWindow();
        int realW = window.getGuiScaledWidth();
        int realH = window.getGuiScaledHeight();
        float fitW = (realW - MARGIN) / (float) designWidth();
        float fitH = (realH - MARGIN) / (float) designHeight();
        this.uiScale = Math.max(MIN_SCALE, Math.min(1.0f, Math.min(fitW, fitH)));
        this.width = (int) Math.floor(realW / uiScale);
        this.height = (int) Math.floor(realH / uiScale);
        super.init();
    }

    private boolean scaled() {
        return uiScale < 0.999f;
    }

    @Override
    public final void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (!scaled()) {
            renderScaled(g, mouseX, mouseY, partialTick);
            return;
        }
        g.pose().pushPose();
        g.pose().scale(uiScale, uiScale, 1.0f);
        renderScaled(g, (int) (mouseX / uiScale), (int) (mouseY / uiScale), partialTick);
        g.pose().popPose();
    }

    protected void renderScaled(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
    }

    @Override
    public final boolean mouseClicked(double mouseX, double mouseY, int button) {
        return mouseClickedScaled(mouseX / uiScale, mouseY / uiScale, button);
    }

    protected boolean mouseClickedScaled(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public final boolean mouseReleased(double mouseX, double mouseY, int button) {
        return mouseReleasedScaled(mouseX / uiScale, mouseY / uiScale, button);
    }

    protected boolean mouseReleasedScaled(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public final boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return mouseDraggedScaled(mouseX / uiScale, mouseY / uiScale, button, dragX / uiScale, dragY / uiScale);
    }

    protected boolean mouseDraggedScaled(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public final boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return mouseScrolledScaled(mouseX / uiScale, mouseY / uiScale, delta);
    }

    protected boolean mouseScrolledScaled(double mouseX, double mouseY, double delta) {
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public final void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX / uiScale, mouseY / uiScale);
    }
}
