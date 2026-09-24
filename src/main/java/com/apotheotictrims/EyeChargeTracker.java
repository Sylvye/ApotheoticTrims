package com.apotheotictrims;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class EyeChargeTracker {
    private final Map<UUID, State> states = new HashMap<>();

    boolean stare(UUID viewer, UUID target, long tick, long requiredTicks) {
        State state = states.computeIfAbsent(viewer, ignored -> new State());
        if (target == null) {
            state.target = null;
            state.stareTriggered = false;
            return false;
        }
        if (!target.equals(state.target)) {
            state.target = target;
            state.stareStart = tick;
            state.stareTriggered = false;
        }
        if (state.stareTriggered || tick - state.stareStart < requiredTicks) return false;
        state.stareTriggered = true;
        return true;
    }

    boolean sneak(UUID viewer, boolean sneaking, long tick, long requiredTicks) {
        State state = states.computeIfAbsent(viewer, ignored -> new State());
        if (!sneaking) {
            state.sneakStart = -1;
            state.sneakTriggered = false;
            return false;
        }
        if (state.sneakStart < 0) state.sneakStart = tick;
        if (state.sneakTriggered || tick - state.sneakStart < requiredTicks) return false;
        state.sneakTriggered = true;
        return true;
    }

    void clear(UUID viewer) { states.remove(viewer); }
    void clear() { states.clear(); }

    private static final class State {
        UUID target;
        long stareStart;
        boolean stareTriggered;
        long sneakStart = -1;
        boolean sneakTriggered;
    }
}
