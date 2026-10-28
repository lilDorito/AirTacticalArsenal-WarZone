package com.synarsis.airtacticalarsenal.entity;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class EntityDataMirror {
    private static final Field ITEMS = find();

    private final Map<Integer, List<SynchedEntityData.DataValue<?>>> sent = new HashMap<>();

    private static Field find() {
        try {
            return ObfuscationReflectionHelper.findField(SynchedEntityData.class, "f_135345_");
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static List<SynchedEntityData.DataValue<?>> allValues(Entity entity) {
        SynchedEntityData data = entity.getEntityData();
        if (ITEMS != null) {
            try {
                Object map = ITEMS.get(data);
                if (map instanceof Int2ObjectMap<?> items) {
                    List<SynchedEntityData.DataValue<?>> values = new ArrayList<>(items.size());
                    for (Object item : items.values()) {
                        if (item instanceof SynchedEntityData.DataItem<?> dataItem) {
                            values.add(dataItem.value());
                        }
                    }
                    return values;
                }
            } catch (IllegalAccessException | RuntimeException ignored) {
            }
        }
        List<SynchedEntityData.DataValue<?>> values = data.getNonDefaultValues();
        return values != null ? values : List.of();
    }

    public void sync(ServerPlayer viewer, Entity entity) {
        List<SynchedEntityData.DataValue<?>> now = allValues(entity);
        List<SynchedEntityData.DataValue<?>> last = sent.get(entity.getId());
        if (now.isEmpty() || now.equals(last)) {
            return;
        }
        List<SynchedEntityData.DataValue<?>> changed = new ArrayList<>();
        if (last == null || last.size() != now.size()) {
            changed.addAll(now);
        } else {
            for (int i = 0; i < now.size(); i++) {
                if (!now.get(i).equals(last.get(i))) {
                    changed.add(now.get(i));
                }
            }
        }
        sent.put(entity.getId(), now);
        if (!changed.isEmpty()) {
            viewer.connection.send(new ClientboundSetEntityDataPacket(entity.getId(), changed));
        }
    }

    public void forget(int id) {
        sent.remove(id);
    }

    public void mark(Entity entity) {
        sent.put(entity.getId(), allValues(entity));
    }

    public void clear() {
        sent.clear();
    }
}
