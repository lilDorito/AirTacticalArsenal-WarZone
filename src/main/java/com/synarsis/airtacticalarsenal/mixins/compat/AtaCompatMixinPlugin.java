package com.synarsis.airtacticalarsenal.mixins.compat;

import net.minecraftforge.fml.loading.LoadingModList;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class AtaCompatMixinPlugin implements IMixinConfigPlugin {

    private boolean ashVehiclePresent;
    private boolean sbwPresent;

    @Override
    public void onLoad(String mixinPackage) {
        LoadingModList mods = LoadingModList.get();
        ashVehiclePresent = mods != null && mods.getModFileById("ashvehicle") != null;
        sbwPresent = mods != null && mods.getModFileById("superbwarfare") != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.contains(".ashvehicle.")) {
            return ashVehiclePresent;
        }
        if (mixinClassName.contains(".sbw.")) {
            return sbwPresent;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
