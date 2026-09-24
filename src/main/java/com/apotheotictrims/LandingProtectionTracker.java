package com.apotheotictrims;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

final class LandingProtectionTracker {
    private final Map<UUID, Integer> armedAtTick = new HashMap<>();

    void arm(UUID player, int currentTick) { armedAtTick.put(player, currentTick); }
    boolean consume(UUID player) { return armedAtTick.remove(player) != null; }
    void clear(UUID player) { armedAtTick.remove(player); }
    void clear() { armedAtTick.clear(); }

    boolean clearAfterSafeLanding(UUID player, int currentTick, boolean onGround, boolean inWater) {
        Integer armed = armedAtTick.get(player);
        if (armed == null || currentTick <= armed + 2 || (!onGround && !inWater)) return false;
        armedAtTick.remove(player);
        return true;
    }
}
