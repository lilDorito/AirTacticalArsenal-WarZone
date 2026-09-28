package com.synarsis.airtacticalarsenal.client.lang;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.synarsis.airtacticalarsenal.config.AtaClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "ata", bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class AtaLanguage {

    private static final Logger LOGGER = LoggerFactory.getLogger("ATA-Language");
    private static final Map<String, Map<String, String>> CACHE = new HashMap<>();

    private static volatile boolean serverAllowsClientLanguage = true;

    private AtaLanguage() {}

    public static void setServerAllowsClientLanguage(boolean allow) {
        serverAllowsClientLanguage = allow;
        apply();
    }

    public static boolean isServerForcingEnglish() {
        return !serverAllowsClientLanguage;
    }

    public static String effectiveLanguage() {
        return serverAllowsClientLanguage ? AtaClientConfig.language() : AtaClientConfig.EN;
    }

    public static String displayName(String code) {
        return switch (code) {
            case AtaClientConfig.RU -> "Русский";
            case AtaClientConfig.UK -> "Українська";
            default -> "English";
        };
    }

    private static Map<String, String> load(String code) {
        return CACHE.computeIfAbsent(code, c -> {
            String path = AtaClientConfig.EN.equals(c)
                    ? "/assets/ata/lang/en_us.json"
                    : "/assets/ata/lang_optional/" + c + ".json";
            try (InputStream in = AtaLanguage.class.getResourceAsStream(path)) {
                if (in == null) {
                    LOGGER.warn("[ATA] Language file not found: {}", path);
                    return Collections.emptyMap();
                }
                try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    Map<String, String> map = new Gson().fromJson(r, new TypeToken<Map<String, String>>() {}.getType());
                    return map != null ? Collections.unmodifiableMap(map) : Collections.emptyMap();
                }
            } catch (Exception e) {
                LOGGER.error("[ATA] Failed to load language file {}", path, e);
                return Collections.emptyMap();
            }
        });
    }

    public static void apply() {
        Language current = Language.getInstance();
        String wanted = effectiveLanguage();
        if (current instanceof Wrapper w) {
            if (w.code.equals(wanted)) return;
            Language.inject(new Wrapper(w.delegate, wanted, load(wanted), load(AtaClientConfig.EN)));
        } else if (current != null) {
            Language.inject(new Wrapper(current, wanted, load(wanted), load(AtaClientConfig.EN)));
        }
    }

    private static final class Wrapper extends Language {
        final Language delegate;
        final String code;
        final Map<String, String> primary;
        final Map<String, String> english;

        Wrapper(Language delegate, String code, Map<String, String> primary, Map<String, String> english) {
            this.delegate = delegate;
            this.code = code;
            this.primary = primary;
            this.english = english;
        }

        @Override
        public String getOrDefault(String key, String fallback) {
            String v = primary.get(key);
            if (v == null) v = english.get(key);
            return v != null ? v : delegate.getOrDefault(key, fallback);
        }

        @Override
        public boolean has(String key) {
            return primary.containsKey(key) || english.containsKey(key) || delegate.has(key);
        }

        @Override
        public boolean isDefaultRightToLeft() {
            return delegate.isDefaultRightToLeft();
        }

        @Override
        public FormattedCharSequence getVisualOrder(FormattedText text) {
            return delegate.getVisualOrder(text);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) apply();
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        serverAllowsClientLanguage = true;
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("atalang")
                .executes(ctx -> {
                    msg(Component.translatable("message.ata.lang.current", displayName(effectiveLanguage())));
                    msg(Component.translatable("message.ata.lang.usage"));
                    return 1;
                })
                .then(Commands.argument("language", StringArgumentType.word())
                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(AtaClientConfig.LANGUAGES, b))
                        .executes(ctx -> {
                            String code = StringArgumentType.getString(ctx, "language").toLowerCase();
                            if (!AtaClientConfig.LANGUAGES.contains(code)) {
                                msg(Component.translatable("message.ata.lang.usage"));
                                return 0;
                            }
                            AtaClientConfig.LANGUAGE.set(code);
                            AtaClientConfig.SPEC.save();
                            apply();
                            if (isServerForcingEnglish()) {
                                msg(Component.translatable("message.ata.lang.forced", displayName(code)));
                            } else {
                                msg(Component.translatable("message.ata.lang.set", displayName(code)));
                            }
                            return 1;
                        })));
    }

    private static void msg(Component c) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) mc.player.displayClientMessage(c, false);
    }
}
