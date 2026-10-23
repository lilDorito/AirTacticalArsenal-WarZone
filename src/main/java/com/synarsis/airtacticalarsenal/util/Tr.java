package com.synarsis.airtacticalarsenal.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.locale.Language;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IllegalFormatException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Tr {
    private static final String FALLBACK_CODE = "en_us";
    private static final Map<String, Map<String, String>> RAW = new ConcurrentHashMap<>();

    private Tr() {}

    public static String s(String key, Object... args) {
        String translated = Language.getInstance().getOrDefault(key);
        String raw = rawPattern(key);
        if (raw != null) {
            try {
                return String.format(raw, args);
            } catch (IllegalFormatException ignored) {
            }
        }
        try {
            return String.format(translated, args);
        } catch (IllegalFormatException e) {
            return translated;
        }
    }

    private static String rawPattern(String key) {
        String code = currentCode();
        String value = RAW.computeIfAbsent(code, Tr::load).get(key);
        if (value == null && !FALLBACK_CODE.equals(code)) {
            value = RAW.computeIfAbsent(FALLBACK_CODE, Tr::load).get(key);
        }
        return value;
    }

    private static String currentCode() {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) {
            try {
                String selected = TrClient.selectedCode();
                if (selected != null && !selected.isEmpty()) {
                    return selected.toLowerCase(java.util.Locale.ROOT);
                }
            } catch (RuntimeException | LinkageError ignored) {
            }
        }
        return FALLBACK_CODE;
    }

    private static Map<String, String> load(String code) {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = Tr.class.getResourceAsStream("/assets/ata/lang/" + code + ".json")) {
            if (in == null) {
                return out;
            }
            JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
                if (entry.getValue().isJsonPrimitive()) {
                    out.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }
}
