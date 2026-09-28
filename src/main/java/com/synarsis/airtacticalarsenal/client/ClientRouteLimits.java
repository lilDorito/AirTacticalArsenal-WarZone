package com.synarsis.airtacticalarsenal.client;

public final class ClientRouteLimits {

    private static volatile int shahedMax = 10000;
    private static volatile int orlanMax = 10000;

    private ClientRouteLimits() {}

    public static void set(int shahed, int orlan) {
        shahedMax = shahed;
        orlanMax = orlan;
    }

    public static int shahed() {
        return shahedMax;
    }

    public static int orlan() {
        return orlanMax;
    }
}
