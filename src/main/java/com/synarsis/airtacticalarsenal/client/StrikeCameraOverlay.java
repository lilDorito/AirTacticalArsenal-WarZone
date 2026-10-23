package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.entity.ShahedEntity;
import com.synarsis.airtacticalarsenal.util.Tr;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class StrikeCameraOverlay {
    private static final int FRAME = 0xCCFF5533;
    private static final int TEXT = 0xFFFFCCBB;
    private static final int DIM = 0xFFAA8877;
    private static final int PANEL = 0x66000000;

    private StrikeCameraOverlay() {
    }

    public static void render(GuiGraphics g, int width, int height, ShahedEntity drone) {
        Minecraft mc = Minecraft.getInstance();
        OrlanCameraOverlay.renderNightVision(g, mc, width, height);

        int border = 32;
        g.fill(0, 0, width, border, 0xDD000000);
        g.fill(0, height - border, width, height, 0xDD000000);
        g.fill(0, border, border, height - border, 0xDD000000);
        g.fill(width - border, border, width, height - border, 0xDD000000);
        g.fill(border, border, width - border, border + 1, FRAME);
        g.fill(border, height - border - 1, width - border, height - border, FRAME);
        g.fill(border, border, border + 1, height - border, FRAME);
        g.fill(width - border - 1, border, width - border, height - border, FRAME);

        int cx = width / 2;
        int cy = height / 2;
        g.fill(cx - 14, cy, cx - 4, cy + 1, FRAME);
        g.fill(cx + 5, cy, cx + 15, cy + 1, FRAME);
        g.fill(cx, cy - 14, cx + 1, cy - 4, FRAME);
        g.fill(cx, cy + 5, cx + 1, cy + 15, FRAME);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFF2222);

        int left = border + 6;
        int top = border + 6;
        Vec3 pos = drone.position();
        BlockPos target = drone.getStrikeTarget();
        double toTarget = Math.sqrt(Math.pow(target.getX() + 0.5 - pos.x, 2) + Math.pow(target.getZ() + 0.5 - pos.z, 2));

        String header = Tr.s("hud.ata.strike.header", drone.getDroneName().getString().toUpperCase(java.util.Locale.ROOT));
        g.fill(left - 2, top - 2, left + mc.font.width(header) + 2, top + 10, PANEL);
        g.drawString(mc.font, header, left, top, TEXT, true);
        top += 16;
        g.drawString(mc.font, String.format("X: %d  Y: %d  Z: %d", (int) pos.x, (int) pos.y, (int) pos.z), left, top, DIM, true);
        top += 12;
        g.drawString(mc.font, Tr.s("hud.ata.strike.target", target.getX(), target.getY(), target.getZ()), left, top, TEXT, true);
        top += 12;
        g.drawString(mc.font, Tr.s("hud.ata.orlan.to_target", toTarget), left, top, TEXT, true);
        top += 12;
        g.drawString(mc.font, Tr.s(phaseKey(drone.getFlightPhase())), left, top,
                drone.getFlightPhase() == ShahedEntity.FlightPhase.DIVING ? 0xFFFF3333 : DIM, true);
        top += 12;
        g.drawString(mc.font, String.format("HP: %.0f%%", drone.getHealthPercent() * 100.0F), left, top, DIM, true);

        if (StrikeCameraClient.showNeedLock()) {
            String need = Tr.s("hud.ata.strike.need_lock");
            g.drawString(mc.font, need, cx - mc.font.width(need) / 2, cy + 40, 0xFFFF4444, true);
        } else if (drone.isAttackCommanded() && StrikeCameraClient.lastSent() != null) {
            Vec3 sent = StrikeCameraClient.lastSent();
            String diving = Tr.s("hud.ata.strike.diving_on", (int) sent.x, (int) sent.y, (int) sent.z, pos.distanceTo(sent));
            g.drawString(mc.font, diving, cx - mc.font.width(diving) / 2, cy + 40, 0xFFFF4444, true);
        }

        String state;
        int stateColor;
        if (drone.isAttackCommanded()) {
            state = Tr.s("hud.ata.strike.state.attack");
            stateColor = 0xFFFF3333;
        } else if (drone.isLoitering()) {
            int seconds = drone.loiterTicksLeft() / 20;
            state = Tr.s("hud.ata.strike.state.loiter", String.format("%02d:%02d", seconds / 60, seconds % 60));
            stateColor = 0xFFFFDD55;
        } else {
            state = Tr.s("hud.ata.strike.state.route");
            stateColor = DIM;
        }
        g.drawString(mc.font, state, cx - mc.font.width(state) / 2, border + 30, stateColor, true);

        String hint = Tr.s("hud.ata.strike.controls", StrikeCameraClient.diveHint().getString(),
                OrlanCameraHandler.visionName());
        int bottom = height - border - 14;
        g.fill(cx - mc.font.width(hint) / 2 - 3, bottom - 2, cx + mc.font.width(hint) / 2 + 3, bottom + 10, PANEL);
        g.drawString(mc.font, hint, cx - mc.font.width(hint) / 2, bottom, DIM, true);

        double speed = drone.getDeltaMovement().length() * 20.0;
        String spd = String.format("SPD: %.0f b/s", speed);
        g.drawString(mc.font, spd, width - border - 6 - mc.font.width(spd), border + 6, DIM, true);
        if (OrlanCameraHandler.isNvgActive()) {
            String vision = OrlanCameraHandler.visionName();
            int vx = width - border - 6 - mc.font.width(vision);
            g.fill(vx - 3, border + 17, width - border - 3, border + 29, 0x88003300);
            g.drawString(mc.font, vision, vx, border + 19, 0xFF00FF00, true);
        }

        OrlanCameraOverlay.renderCompass(g, mc, cx, border + 14, OrlanCameraHandler.getCameraYaw());
        OrlanCameraOverlay.renderLockOnUI(g, mc, cx, cy - 60, pos, width);
    }

    private static String phaseKey(ShahedEntity.FlightPhase phase) {
        return switch (phase) {
            case WARMUP, LAUNCHING -> "hud.ata.orlan.phase.launching";
            case CLIMBING -> "hud.ata.orlan.phase.climbing";
            case CRUISING -> "hud.ata.orlan.phase.cruising";
            case DIVING -> "hud.ata.strike.phase.diving";
        };
    }
}
