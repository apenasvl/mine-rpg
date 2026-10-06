package com.rpgstats.gui;

import java.util.HashMap;
import java.util.Map;

/** Each permanent-choice page retains its own preview and in-flight confirmation. */
public final class ProgressionSelectionState<P,T> {
    private final Map<P,ClassSelectionState<T>> pages=new HashMap<>();
    public ClassSelectionState<T> forPage(P page) {
        return pages.computeIfAbsent(page,ignored->new ClassSelectionState<>());
    }
}
