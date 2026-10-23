package com.synarsis.airtacticalarsenal.radar.vehicle;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

public final class VehicleRadarProfiles extends SimpleJsonResourceReloadListener {

    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("ATA-VehicleRadar");
    private static final Gson GSON = new GsonBuilder().create();
    public static final VehicleRadarProfiles INSTANCE = new VehicleRadarProfiles();

    private volatile Map<ResourceLocation, VehicleRadarProfile> radars = Map.of();
    private volatile Map<ResourceLocation, Float> signatures = Map.of();

    private VehicleRadarProfiles() {
        super(GSON, "vehicle_radars");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        Map<ResourceLocation, VehicleRadarProfile> nextRadars = new HashMap<>();
        Map<ResourceLocation, Float> nextSignatures = new HashMap<>();
        for (Map.Entry<ResourceLocation, JsonElement> entry : files.entrySet()) {
            try {
                JsonObject json = GsonHelper.convertToJsonObject(entry.getValue(), entry.getKey().toString());
                if (json.has("signatures")) {
                    for (Map.Entry<String, JsonElement> sig : GsonHelper.getAsJsonObject(json, "signatures").entrySet()) {
                        ResourceLocation id = ResourceLocation.tryParse(sig.getKey());
                        if (id != null) nextSignatures.put(id, Math.max(0.0f, sig.getValue().getAsFloat()));
                    }
                }
                if (!json.has("vehicles")) continue;
                JsonArray vehicles = GsonHelper.getAsJsonArray(json, "vehicles");
                VehicleRadarProfile profile = json.has("radar") && json.get("radar").isJsonObject()
                        ? VehicleRadarProfile.fromJson(json.getAsJsonObject("radar")) : null;
                boolean noRadar = json.has("radar") && json.get("radar").isJsonNull();
                Float signature = json.has("signature") ? GsonHelper.getAsFloat(json, "signature") : null;
                for (JsonElement v : vehicles) {
                    ResourceLocation id = ResourceLocation.tryParse(v.getAsString());
                    if (id == null) continue;
                    if (profile != null) nextRadars.put(id, profile);
                    else if (noRadar) nextRadars.remove(id);
                    if (signature != null) nextSignatures.put(id, Math.max(0.0f, signature));
                }
            } catch (Exception e) {
                LOGGER.warn("Bad vehicle radar file {}: {}", entry.getKey(), e.getMessage());
            }
        }
        this.radars = nextRadars;
        this.signatures = nextSignatures;
    }

    public VehicleRadarProfile radarFor(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        return id == null ? null : radars.get(id);
    }

    public float signatureFor(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) return 1.0f;
        Float s = signatures.get(id);
        return s == null ? 1.0f : s;
    }

    public Map<ResourceLocation, VehicleRadarProfile> radars() {
        return radars;
    }

    public Map<ResourceLocation, Float> signatures() {
        return signatures;
    }

    private static volatile Map<ResourceLocation, VehicleRadarProfile> clientRadars = Map.of();
    private static volatile Map<ResourceLocation, Float> clientSignatures = Map.of();

    public static void setClientData(Map<ResourceLocation, VehicleRadarProfile> radars, Map<ResourceLocation, Float> signatures) {
        clientRadars = Map.copyOf(radars);
        clientSignatures = Map.copyOf(signatures);
    }

    public static VehicleRadarProfile clientRadarFor(ResourceLocation id) {
        return id == null ? null : clientRadars.get(id);
    }

    public static float clientSignatureFor(ResourceLocation id) {
        Float s = id == null ? null : clientSignatures.get(id);
        return s == null ? 1.0f : s;
    }

    public boolean hasAnyRadar() {
        return !radars.isEmpty();
    }
}
