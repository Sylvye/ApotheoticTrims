package com.apotheotictrims;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ComboTracker {
    private static final int HIT_INTERVAL_TICKS = 10;
    private final Map<UUID, Combo> combos = new HashMap<>();

    public Result hit(UUID player, long nowMillis, int nowTick, int threshold, long timeoutMillis) {
        Combo previous = combos.get(player);
        if (previous != null && nowTick - previous.lastTick() < HIT_INTERVAL_TICKS) {
            return new Result(previous.count(), false);
        }
        int count = previous == null || nowMillis - previous.lastHit() > timeoutMillis ? 1 : previous.count() + 1;
        if (count >= threshold) {
            combos.put(player, new Combo(0, nowMillis, nowTick));
            return new Result(threshold, true);
        }
        combos.put(player, new Combo(count, nowMillis, nowTick));
        return new Result(count, false);
    }

    public void clear(UUID player) { combos.remove(player); }
    public void clear() { combos.clear(); }
    public int count(UUID player) { return combos.containsKey(player) ? combos.get(player).count() : 0; }

    private record Combo(int count, long lastHit, int lastTick) {}
    public record Result(int count, boolean triggered) {}
}
