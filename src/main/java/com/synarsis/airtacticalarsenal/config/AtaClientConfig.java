package com.synarsis.airtacticalarsenal.config;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class AtaClientConfig {

    public static final String EN = "en_us";
    public static final String RU = "ru_ru";
    public static final String UK = "uk_ua";
    public static final List<String> LANGUAGES = List.of(EN, RU, UK);

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> LANGUAGE;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("Language");
        LANGUAGE = b.comment(
                        "Language for Air Tactical Arsenal texts (screens, HUD, tooltips, messages).",
                        "Independent of the Minecraft language setting. English is the default.",
                        "Allowed: en_us (English), ru_ru (Russian), uk_ua (Ukrainian).",
                        "Servers can force English for everyone (allowClientLanguage = false in ata-common.toml).",
                        "Can also be changed in game with /atalang <en_us|ru_ru|uk_ua>.")
                .defineInList("language", EN, LANGUAGES);
        b.pop();
        SPEC = b.build();
    }

    private AtaClientConfig() {}

    public static String language() {
        try {
            String v = LANGUAGE.get();
            return LANGUAGES.contains(v) ? v : EN;
        } catch (IllegalStateException notLoaded) {
            return EN;
        }
    }
}
