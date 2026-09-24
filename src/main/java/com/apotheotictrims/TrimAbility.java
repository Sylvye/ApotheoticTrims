package com.apotheotictrims;

import org.bukkit.Material;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public enum TrimAbility {
    TIDE("Tide", Material.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, "Dolphin's Grace and Resistance while in water",
            level("dolphins-grace-level", "Dolphin's Grace level", 1), level("resistance-level", "Resistance level", 1)),
    COAST("Coast", Material.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, "Faster mounts while riding",
            decimal("land-multiplier", "Land mount speed", 1.3, 1, 3, .05, .25),
            decimal("nautilus-multiplier", "Nautilus speed", 1.3, 1, 3, .05, .25),
            decimal("happy-ghast-multiplier", "Happy ghast speed", 1.3, 1, 3, .05, .25)),
    DUNE("Dune", Material.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, "Configurable knockback resistance",
            decimal("knockback-resistance", "Knockback resistance", .4, 0, 1, .05, .1)),
    WILD("Wild", Material.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, "One double jump per airborne cycle",
            decimal("vertical-velocity", "Vertical velocity", .9, 0, 10, .05, .25),
            decimal("cooldown-seconds", "Cooldown seconds", 0, 0, 600, .5, 5)),
    SENTRY("Sentry", Material.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, "Increased projectile damage",
            decimal("damage-multiplier", "Damage multiplier", 1.3, 0, 10, .05, .25)),
    SNOUT("Snout", Material.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, "Hostile mobs ignore you until provoked",
            decimal("retaliation-seconds", "Retaliation seconds", 15, .1, 600, .5, 5)),
    BOLT("Bolt", Material.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, "Melee combos deal bonus damage and call down visual lightning",
            integer("combo-hits", "Combo hits", 10, 1, 1000, 1, 10),
            decimal("timeout-seconds", "Timeout seconds", 5, .1, 600, .5, 5),
            decimal("bonus-damage", "Bonus damage", 10, 0, 100, .5, 2)),
    FLOW("Flow", Material.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, "A disabled shield violently repels nearby entities",
            decimal("radius", "Radius", 6, 0, 64, .5, 2),
            decimal("horizontal-velocity", "Horizontal velocity", 2, 0, 10, .1, .5),
            decimal("vertical-velocity", "Vertical velocity", .7, 0, 10, .05, .25)),
    RIB("Rib", Material.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, "Immune to fire damage; Regeneration I while burning by default",
            level("regeneration-level", "Burning Regeneration level", 1)),
    WARD("Ward", Material.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, "Sculk sensors and Wardens cannot hear you"),
    VEX("Vex", Material.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, "Totem activations grant Strength and Speed",
            level("strength-level", "Strength level", 2), level("speed-level", "Speed level", 2),
            decimal("duration-seconds", "Duration seconds", 90, .1, 600, .5, 5)),
    SPIRE("Spire", Material.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, "Slow Falling while sneaking in air; void falls wrap to world height"),
    EYE("Eye", Material.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, "Stare to slow and reveal; sneak to reveal nearby players",
            decimal("radius", "Sneak reveal radius", 10, 0, 64, .5, 2),
            decimal("stare-seconds", "Stare charge seconds", 5, .1, 600, .5, 5),
            decimal("stare-effect-seconds", "Stare effect seconds", 15, .1, 600, .5, 5),
            level("slowness-level", "Stare Slowness level", 1),
            decimal("sneak-seconds", "Sneak charge seconds", 5, .1, 600, .5, 5),
            decimal("reveal-seconds", "Sneak reveal seconds", 10, .1, 600, .5, 5)),
    SILENCE("Silence", Material.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, "Specified negative effects cannot affect you"),
    WAYFINDER("Wayfinder", Material.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, "Permanent Speed",
            level("speed-level", "Speed level", 2)),
    RAISER("Raiser", Material.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, "Permanent Jump Boost",
            level("jump-boost-level", "Jump Boost level", 2)),
    SHAPER("Shaper", Material.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, "Permanent Haste",
            level("haste-level", "Haste level", 2)),
    HOST("Host", Material.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, "Permanent Hero of the Village and Luck",
            level("hero-level", "Hero of the Village level", 5),
            level("luck-level", "Luck level", 1));

    private final String displayName;
    private final Material icon;
    private final String description;
    private final List<SettingSpec> settings;

    TrimAbility(String displayName, Material icon, String description, SettingSpec... settings) {
        this.displayName = displayName;
        this.icon = icon;
        this.description = description;
        this.settings = List.of(settings);
    }

    public String key() { return name().toLowerCase(Locale.ROOT); }
    public String displayName() { return displayName; }
    public Material icon() { return icon; }
    public String description() { return description; }
    public List<SettingSpec> settings() { return settings; }

    public Optional<SettingSpec> setting(String key) {
        return settings.stream().filter(spec -> spec.key().equalsIgnoreCase(key)).findFirst();
    }

    public static Optional<TrimAbility> fromKey(String key) {
        return Arrays.stream(values()).filter(value -> value.key().equalsIgnoreCase(key)).findFirst();
    }

    private static SettingSpec level(String key, String label, int defaultValue) {
        return integer(key, label, defaultValue, 1, 10, 1, 2);
    }

    private static SettingSpec integer(String key, String label, int defaultValue, int min, int max, int step, int coarse) {
        return new SettingSpec(key, label, defaultValue, min, max, step, coarse, true);
    }

    private static SettingSpec decimal(String key, String label, double defaultValue, double min, double max,
                                       double step, double coarse) {
        return new SettingSpec(key, label, defaultValue, min, max, step, coarse, false);
    }
}
