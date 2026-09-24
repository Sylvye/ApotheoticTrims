package com.apotheotictrims;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class SilenceEffects {
    private SilenceEffects() {}

    public static final Set<String> BLOCKED_KEYS = Set.of(
            "unluck", "blindness", "darkness", "hunger", "infested", "instant_damage", "levitation",
            "slow_falling", "mining_fatigue", "nausea", "poison", "slowness", "weakness", "wither"
    );

    public static boolean isBlocked(PotionEffectType type) {
        return BLOCKED_KEYS.contains(Registry.MOB_EFFECT.getKeyOrThrow(type).getKey());
    }

    public static Set<PotionEffectType> types() {
        return BLOCKED_KEYS.stream()
                .map(key -> Registry.MOB_EFFECT.get(NamespacedKey.minecraft(key)))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }
}
