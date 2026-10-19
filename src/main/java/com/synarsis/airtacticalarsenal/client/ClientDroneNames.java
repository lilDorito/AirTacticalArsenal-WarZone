package com.synarsis.airtacticalarsenal.client;

import com.synarsis.airtacticalarsenal.util.Tr;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class ClientDroneNames {
    private static final Map<Integer, String> KEYS = new ConcurrentHashMap<>();

    private ClientDroneNames() {
    }

    public static void putAll(Map<Integer, String> names) {
        if (names != null && !names.isEmpty()) {
            if (KEYS.size() > 512) {
                KEYS.clear();
            }
            KEYS.putAll(names);
        }
    }

    public static String name(int entityId, String fallbackKey) {
        String key = KEYS.get(entityId);
        if (key == null && Minecraft.getInstance().level != null) {
            Entity e = Minecraft.getInstance().level.getEntity(entityId);
            if (e instanceof com.synarsis.airtacticalarsenal.entity.OrlanEntity orlan) {
                key = orlan.getDroneNameKey();
            } else if (e instanceof com.synarsis.airtacticalarsenal.entity.ShahedEntity shahed) {
                key = shahed.getDroneNameKey();
            }
        }
        return Tr.s(key != null ? key : fallbackKey);
    }
}
