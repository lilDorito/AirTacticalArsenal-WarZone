package com.synarsis.airtacticalarsenal.mixins.compat.sbw;

import com.synarsis.airtacticalarsenal.client.shader.SbwThermalBridge;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.atsuishio.superbwarfare.event.ClientEventHandler", remap = false)
public abstract class SbwThermalMixin {

    @Inject(method = "handleThermalImaging(Lnet/minecraft/world/entity/player/Player;)V",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void ata$keepCameraThermal(Player player, CallbackInfo ci) {
        if (SbwThermalBridge.isActive()) {
            ci.cancel();
        }
    }

    @Inject(method = "hasThermalImagingGoggles()Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void ata$cameraThermalCounts(CallbackInfoReturnable<Boolean> cir) {
        if (SbwThermalBridge.isActive()) {
            cir.setReturnValue(true);
        }
    }
}
