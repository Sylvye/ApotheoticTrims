package com.apotheotictrims;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

public final class SettingsManager {
    public static final int SCHEMA_VERSION = 9;
    private final Path dataDirectory;
    private final Logger logger;
    private final Map<TrimAbility, Boolean> enabled = new EnumMap<>(TrimAbility.class);
    private final Map<TrimAbility, Map<String, Double>> values = new EnumMap<>(TrimAbility.class);
    private boolean actionBar = true;
    private boolean sounds = true;
    private boolean particles = true;

    public SettingsManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        resetAllInMemory();
    }

    public void load() {
        resetAllInMemory();
        Path file = dataDirectory.resolve("settings.yml");
        if (!Files.exists(file)) {
            save();
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file.toFile());
        int loadedSchema = yaml.getInt("schema-version", 1);
        actionBar = yaml.getBoolean("feedback.action-bar", true);
        sounds = yaml.getBoolean("feedback.sounds", true);
        particles = yaml.getBoolean("feedback.particles", true);
        for (TrimAbility ability : TrimAbility.values()) {
            String base = "abilities." + ability.key();
            enabled.put(ability, yaml.getBoolean(base + ".enabled", true));
            for (SettingSpec spec : ability.settings()) {
                double read = yaml.getDouble(base + ".values." + spec.key(), spec.defaultValue());
                try {
                    values.get(ability).put(spec.key(), spec.validate(read));
                } catch (IllegalArgumentException ex) {
                    logger.warning("Invalid saved setting " + base + "." + spec.key() + "; using default.");
                }
            }
        }
        boolean migrated = false;
        if (loadedSchema < 2) { migrateVersionTwo(); migrated = true; }
        if (loadedSchema < 3) { migrateVersionThree(); migrated = true; }
        if (loadedSchema < 4) { migrateVersionFour(yaml); migrated = true; }
        if (loadedSchema < 5) migrated = true;
        if (loadedSchema < 6) migrated = true;
        if (loadedSchema < 7) { migrateVersionSeven(yaml, loadedSchema); migrated = true; }
        if (loadedSchema < 8) migrated = true;
        if (loadedSchema < 9) { migrateVersionNine(); migrated = true; }
        if (migrated) save();
    }

    public synchronized void save() {
        try {
            Files.createDirectories(dataDirectory);
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("schema-version", SCHEMA_VERSION);
            yaml.set("feedback.action-bar", actionBar);
            yaml.set("feedback.sounds", sounds);
            yaml.set("feedback.particles", particles);
            for (TrimAbility ability : TrimAbility.values()) {
                String base = "abilities." + ability.key();
                yaml.set(base + ".enabled", enabled.get(ability));
                values.get(ability).forEach((key, value) -> yaml.set(base + ".values." + key, value));
            }
            Path target = dataDirectory.resolve("settings.yml");
            Path temporary = dataDirectory.resolve("settings.yml.tmp");
            yaml.save(temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save settings", ex);
        }
    }

    public boolean enabled(TrimAbility ability) { return enabled.get(ability); }
    public double value(TrimAbility ability, String key) { return values.get(ability).get(key); }
    public int intValue(TrimAbility ability, String key) { return (int) Math.round(value(ability, key)); }
    public boolean actionBar() { return actionBar; }
    public boolean sounds() { return sounds; }
    public boolean particles() { return particles; }

    public void setEnabled(TrimAbility ability, boolean value) {
        boolean previous = enabled.get(ability);
        enabled.put(ability, value);
        try { save(); } catch (RuntimeException ex) { enabled.put(ability, previous); throw ex; }
    }

    public void toggle(TrimAbility ability) { setEnabled(ability, !enabled(ability)); }

    public void setValue(TrimAbility ability, String key, double value) {
        SettingSpec spec = ability.setting(key).orElseThrow(() -> new IllegalArgumentException("Unknown setting: " + key));
        double validated = spec.validate(value);
        Double previous = values.get(ability).put(spec.key(), validated);
        try { save(); } catch (RuntimeException ex) { values.get(ability).put(spec.key(), previous); throw ex; }
    }

    public void setFeedback(String key, boolean value) {
        boolean previous = feedback(key);
        switch (key.toLowerCase(java.util.Locale.ROOT)) {
            case "action-bar" -> actionBar = value;
            case "sounds" -> sounds = value;
            case "particles" -> particles = value;
            default -> throw new IllegalArgumentException("Unknown feedback setting: " + key);
        }
        try { save(); } catch (RuntimeException ex) {
            switch (key.toLowerCase(java.util.Locale.ROOT)) {
                case "action-bar" -> actionBar = previous;
                case "sounds" -> sounds = previous;
                case "particles" -> particles = previous;
                default -> { }
            }
            throw ex;
        }
    }

    public boolean feedback(String key) {
        return switch (key.toLowerCase(java.util.Locale.ROOT)) {
            case "action-bar" -> actionBar;
            case "sounds" -> sounds;
            case "particles" -> particles;
            default -> throw new IllegalArgumentException("Unknown feedback setting: " + key);
        };
    }

    public void reset(TrimAbility ability) {
        boolean previousEnabled = enabled.get(ability);
        Map<String, Double> previousValues = new LinkedHashMap<>(values.get(ability));
        enabled.put(ability, true);
        Map<String, Double> abilityValues = values.get(ability);
        ability.settings().forEach(spec -> abilityValues.put(spec.key(), spec.defaultValue()));
        try { save(); } catch (RuntimeException ex) {
            enabled.put(ability, previousEnabled);
            values.put(ability, previousValues);
            throw ex;
        }
    }

    public void resetAll() {
        resetAllInMemory();
        save();
    }

    private void resetAllInMemory() {
        enabled.clear();
        values.clear();
        for (TrimAbility ability : TrimAbility.values()) {
            enabled.put(ability, true);
            Map<String, Double> defaults = new LinkedHashMap<>();
            ability.settings().forEach(spec -> defaults.put(spec.key(), spec.defaultValue()));
            values.put(ability, defaults);
        }
        actionBar = sounds = particles = true;
    }

    private void migrateVersionTwo() {
        Map<String, Double> vex = values.get(TrimAbility.VEX);
        if (Double.compare(vex.get("duration-seconds"), 15.0) == 0) vex.put("duration-seconds", 90.0);
    }

    private void migrateVersionThree() {
        Map<String, Double> sentry = values.get(TrimAbility.SENTRY);
        if (Double.compare(sentry.get("damage-multiplier"), 1.5) == 0) sentry.put("damage-multiplier", 1.3);
        Map<String, Double> bolt = values.get(TrimAbility.BOLT);
        if (Double.compare(bolt.get("combo-hits"), 20.0) == 0) bolt.put("combo-hits", 10.0);
        Map<String, Double> wayfinder = values.get(TrimAbility.WAYFINDER);
        if (Double.compare(wayfinder.get("speed-level"), 1.0) == 0) wayfinder.put("speed-level", 2.0);
        Map<String, Double> shaper = values.get(TrimAbility.SHAPER);
        if (Double.compare(shaper.get("haste-level"), 1.0) == 0) shaper.put("haste-level", 2.0);
        Map<String, Double> host = values.get(TrimAbility.HOST);
        if (Double.compare(host.get("hero-level"), 1.0) == 0) host.put("hero-level", 5.0);
    }

    private void migrateVersionFour(YamlConfiguration yaml) {
        String oldEyeGlow = "abilities.eye.values.glow-seconds";
        if (!yaml.contains(oldEyeGlow)) return;
        double oldDuration = yaml.getDouble(oldEyeGlow);
        if (Double.compare(oldDuration, 15.0) == 0) return;
        SettingSpec reveal = TrimAbility.EYE.setting("reveal-seconds").orElseThrow();
        try {
            values.get(TrimAbility.EYE).put(reveal.key(), reveal.validate(oldDuration));
        } catch (IllegalArgumentException ex) {
            logger.warning("Invalid saved Eye glow duration; using the new reveal default.");
        }
    }

    private void migrateVersionSeven(YamlConfiguration yaml, int loadedSchema) {
        String oldKey = "abilities.bolt.values.lightning-damage";
        String newKey = "abilities.bolt.values.bonus-damage";
        if (!yaml.contains(oldKey) || yaml.contains(newKey)) return;
        double oldValue = yaml.getDouble(oldKey);
        double migrated = Double.compare(oldValue, 16.0) == 0
                || loadedSchema < 2 && Double.compare(oldValue, 4.0) == 0
                || loadedSchema == 2 && Double.compare(oldValue, 12.0) == 0 ? 10.0 : oldValue;
        SettingSpec bonus = TrimAbility.BOLT.setting("bonus-damage").orElseThrow();
        try {
            values.get(TrimAbility.BOLT).put(bonus.key(), bonus.validate(migrated));
        } catch (IllegalArgumentException ex) {
            logger.warning("Invalid saved Bolt lightning damage; using the new bonus default.");
        }
    }

    private void migrateVersionNine() {
        Map<String, Double> rib = values.get(TrimAbility.RIB);
        if (Double.compare(rib.get("regeneration-level"), 2.0) == 0) rib.put("regeneration-level", 1.0);
    }
}
