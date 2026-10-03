package com.rpgstats.gui;

import java.util.Objects;
import java.util.function.Consumer;

/** Client-only preview state. Only an explicit confirmation sends a class choice. */
public final class ClassSelectionState<T> {
    private T selected;
    private boolean pending;
    private boolean timedOut;
    private long deadline;

    public T selected() { return selected; }
    public boolean pending() { return pending; }
    public boolean timedOut() { return timedOut; }

    public void select(T value) {
        if (pending) return;
        selected = Objects.requireNonNull(value);
        timedOut = false;
    }

    public boolean confirm(long now, Consumer<T> send) {
        if (selected == null || pending) return false;
        pending = true;
        timedOut = false;
        deadline = now + 8000;
        try {
            send.accept(selected);
        } catch (RuntimeException error) {
            pending = false;
            throw error;
        }
        return true;
    }

    public void tick(long now) {
        if (pending && now >= deadline) {
            pending = false;
            timedOut = true;
        }
    }

    public void reset() {
        selected = null;
        pending = false;
        timedOut = false;
    }
}
