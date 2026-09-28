package com.synarsis.airtacticalarsenal.util;

import net.minecraft.locale.Language;

import java.util.IllegalFormatException;

public final class Tr {

    private Tr() {}

    public static String s(String key, Object... args) {
        String pattern = Language.getInstance().getOrDefault(key);
        try {
            return String.format(pattern, args);
        } catch (IllegalFormatException e) {
            return pattern;
        }
    }
}
