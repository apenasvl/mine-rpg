package com.rpgstats.compat;

/**
 * Test-only standalone environment for the dependency-free House domain suite.
 * The production CompatManager depends on Minecraft server classes, which are
 * intentionally excluded from this small javac harness.
 */
public final class CompatManager {
    private CompatManager() {}

    public static boolean isActive(String modId) {
        return false;
    }
}
