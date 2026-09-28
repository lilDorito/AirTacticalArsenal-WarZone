package com.synarsis.airtacticalarsenal.mixins.compat.ashvehicle;

import com.synarsis.airtacticalarsenal.compat.AshVehicleLockRules;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "Aru.Aru.ashvehicle.entity.Class.BaseAircraftEntity", remap = false)
public abstract class AshAircraftLockMixin {

    @Inject(method = "isValidLockTarget(Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void ata$lockAtaDrones(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        Boolean decision = AshVehicleLockRules.decide(entity);
        if (decision != null) {
            cir.setReturnValue(decision);
        }
    }
}
