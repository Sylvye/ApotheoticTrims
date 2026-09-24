package com.apotheotictrims;

import org.bukkit.entity.*;

enum CoastMountCategory {
    LAND("land-multiplier"),
    NAUTILUS("nautilus-multiplier"),
    HAPPY_GHAST("happy-ghast-multiplier");

    private final String settingKey;

    CoastMountCategory(String settingKey) { this.settingKey = settingKey; }

    String settingKey() { return settingKey; }

    static CoastMountCategory of(Entity entity) {
        if (entity instanceof AbstractNautilus) return NAUTILUS;
        if (entity instanceof HappyGhast) return HAPPY_GHAST;
        if (entity instanceof LivingEntity) return LAND;
        return null;
    }
}
