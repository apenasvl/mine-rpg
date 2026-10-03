package com.rpgstats.compat;

import java.util.Set;

/** Adaptador opcional sem referências a classes do mod externo. */
public interface CompatModule {
    String id();
    Set<String> requiredMods();
    String featureSummary();
    default void register(CompatContext context) {}
}
