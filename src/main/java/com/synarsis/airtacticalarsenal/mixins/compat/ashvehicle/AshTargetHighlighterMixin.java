package com.synarsis.airtacticalarsenal.mixins.compat.ashvehicle;

import com.synarsis.airtacticalarsenal.compat.AshVehicleLockRules;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "Aru.Aru.ashvehicle.init.client.ClientEntityHighlighter", remap = false)
public abstract class AshTargetHighlighterMixin {

    @Inject(method = "isValidTarget(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/EntityType;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void ata$showAtaDrones(Entity entity, Player player, Entity vehicle, EntityType<?> playerType,
                                          CallbackInfoReturnable<Boolean> cir) {
        if (entity == player || entity == vehicle) return;
        Boolean decision = AshVehicleLockRules.decide(entity);
        if (decision != null) {
            cir.setReturnValue(decision);
        }
    }
}
