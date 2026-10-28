package com.synarsis.airtacticalarsenal.entity;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public final class CameraEntityStream {
    private static final int KEEPALIVE_TICKS = 20;

    private final Map<Integer, Entity> tracked = new HashMap<>();
    private final Map<Integer, double[]> sent = new HashMap<>();
    private final EntityDataMirror data = new EntityDataMirror();

    public boolean isEmpty() {
        return tracked.isEmpty();
    }

    public int[] ids() {
        return tracked.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    public void clear() {
        tracked.clear();
        sent.clear();
        data.clear();
    }

    public void removeAll(ServerPlayer viewer) {
        if (viewer != null && !tracked.isEmpty()) {
            viewer.connection.send(new ClientboundRemoveEntitiesPacket(ids()));
        }
        clear();
    }

    public void tick(ServerPlayer viewer, ServerLevel level, Entity drone, double radius, Predicate<Entity> filter, boolean scan) {
        if (scan) {
            AABB box = new AABB(drone.getX() - radius, drone.getY() - 300, drone.getZ() - radius,
                    drone.getX() + radius, drone.getY() + 300, drone.getZ() + radius);
            List<Entity> nearby = level.getEntities(drone, box, filter);
            Set<Integer> current = new HashSet<>();
            for (Entity e : nearby) {
                int id = e.getId();
                if (!tracked.containsKey(id)) {
                    Packet<?> add;
                    try {
                        add = e.getAddEntityPacket();
                    } catch (RuntimeException ex) {
                        continue;
                    }
                    if (add == null) {
                        continue;
                    }
                    viewer.connection.send(add);
                    var values = e.getEntityData().getNonDefaultValues();
                    if (values != null) {
                        viewer.connection.send(new ClientboundSetEntityDataPacket(id, values));
                    }
                    tracked.put(id, e);
                    sent.remove(id);
                    data.mark(e);
                }
                current.add(id);
            }
            Set<Integer> gone = new HashSet<>(tracked.keySet());
            gone.removeAll(current);
            if (!gone.isEmpty()) {
                viewer.connection.send(new ClientboundRemoveEntitiesPacket(gone.stream().mapToInt(Integer::intValue).toArray()));
                tracked.keySet().removeAll(gone);
                sent.keySet().removeAll(gone);
                for (int id : gone) {
                    data.forget(id);
                }
            }
        }
        long now = level.getGameTime();
        Iterator<Entity> it = tracked.values().iterator();
        while (it.hasNext()) {
            Entity e = it.next();
            if (e.isRemoved()) {
                continue;
            }
            if ((now + e.getId()) % 2 == 0) {
                data.sync(viewer, e);
            }
            double[] last = sent.get(e.getId());
            boolean moved = last == null
                    || now - (long) last[5] >= KEEPALIVE_TICKS
                    || sq(e.getX() - last[0]) + sq(e.getY() - last[1]) + sq(e.getZ() - last[2]) > 1.0E-6
                    || Math.abs(e.getYRot() - last[3]) > 0.5
                    || Math.abs(e.getXRot() - last[4]) > 0.5;
            if (!moved) {
                continue;
            }
            viewer.connection.send(new ClientboundTeleportEntityPacket(e));
            viewer.connection.send(new net.minecraft.network.protocol.game.ClientboundRotateHeadPacket(e,
                    (byte) net.minecraft.util.Mth.floor(e.getYHeadRot() * 256.0F / 360.0F)));
            if (e.getDeltaMovement().lengthSqr() > 0.001) {
                viewer.connection.send(new ClientboundSetEntityMotionPacket(e));
            }
            sent.put(e.getId(), new double[]{e.getX(), e.getY(), e.getZ(), e.getYRot(), e.getXRot(), now});
        }
    }

    private static double sq(double v) {
        return v * v;
    }
}
