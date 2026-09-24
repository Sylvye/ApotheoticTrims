package com.apotheotictrims;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class SettingsManagerTest {
    @TempDir Path directory;

    @Test
    void restoresRibRegenerationDefaultAndPreservesHigherCustomLevel() throws Exception {
        Files.writeString(directory.resolve("settings.yml"), """
                schema-version: 7
                abilities:
                  rib:
                    values:
                      regeneration-level: 1
                      fire-resistance-level: 1
                """);
        SettingsManager defaults = new SettingsManager(directory, Logger.getAnonymousLogger());
        defaults.load();
        assertEquals(1, defaults.intValue(TrimAbility.RIB, "regeneration-level"));
        String saved = Files.readString(directory.resolve("settings.yml"));
        assertTrue(saved.contains("schema-version: 12"));
        assertFalse(saved.contains("fire-resistance-level"));

        Path custom = Files.createDirectory(directory.resolve("custom-rib"));
        Files.writeString(custom.resolve("settings.yml"), """
                schema-version: 7
                abilities:
                  rib:
                    values:
                      regeneration-level: 3
                """);
        SettingsManager customized = new SettingsManager(custom, Logger.getAnonymousLogger());
        customized.load();
        assertEquals(3, customized.intValue(TrimAbility.RIB, "regeneration-level"));

        Path previousDefault = Files.createDirectory(directory.resolve("previous-default"));
        Files.writeString(previousDefault.resolve("settings.yml"), """
                schema-version: 8
                abilities:
                  rib:
                    values:
                      regeneration-level: 2
                """);
        SettingsManager upgraded = new SettingsManager(previousDefault, Logger.getAnonymousLogger());
        upgraded.load();
        assertEquals(1, upgraded.intValue(TrimAbility.RIB, "regeneration-level"));
    }

    @Test
    void persistsValuesTogglesFeedbackAndResets() {
        SettingsManager first = new SettingsManager(directory, Logger.getAnonymousLogger());
        first.load();
        first.setValue(TrimAbility.BOLT, "combo-hits", 35);
        first.setValue(TrimAbility.EYE, "weakness-level", 3);
        first.setValue(TrimAbility.EYE, "slowness-level", 2);
        first.setValue(TrimAbility.EYE, "glowing-enabled", 0);
        first.setEnabled(TrimAbility.WARD, false);
        first.setFeedback("particles", false);

        SettingsManager second = new SettingsManager(directory, Logger.getAnonymousLogger());
        second.load();
        assertEquals(35, second.intValue(TrimAbility.BOLT, "combo-hits"));
        assertFalse(second.enabled(TrimAbility.WARD));
        assertFalse(second.particles());

        second.reset(TrimAbility.BOLT);
        assertEquals(10, second.intValue(TrimAbility.BOLT, "combo-hits"));
        assertEquals(10, second.value(TrimAbility.BOLT, "bonus-damage"));
        assertEquals(90, second.value(TrimAbility.VEX, "duration-seconds"));
        assertEquals(90, second.value(TrimAbility.VEX, "fire-resistance-duration-seconds"));
        assertEquals(2, second.intValue(TrimAbility.WAYFINDER, "speed-level"));
        assertEquals(2, second.intValue(TrimAbility.SHAPER, "haste-level"));
        assertEquals(5, second.intValue(TrimAbility.HOST, "hero-level"));
        assertEquals(1, second.intValue(TrimAbility.HOST, "luck-level"));
        assertEquals(1, second.intValue(TrimAbility.RIB, "regeneration-level"));
        assertTrue(TrimAbility.RIB.setting("fire-resistance-level").isEmpty());
        for (CoastMountCategory category : CoastMountCategory.values())
            assertEquals(1.3, second.value(TrimAbility.COAST, category.settingKey()));
        assertEquals(3, second.intValue(TrimAbility.EYE, "weakness-level"));
        assertEquals(2, second.intValue(TrimAbility.EYE, "slowness-level"));
        assertEquals(0, second.intValue(TrimAbility.EYE, "glowing-enabled"));
        assertThrows(IllegalArgumentException.class,
                () -> second.setValue(TrimAbility.EYE, "glowing-enabled", 2));
        second.reset(TrimAbility.EYE);
        assertEquals(1, second.intValue(TrimAbility.EYE, "weakness-level"));
        assertEquals(1, second.intValue(TrimAbility.EYE, "slowness-level"));
        assertEquals(1, second.intValue(TrimAbility.EYE, "glowing-enabled"));
        assertEquals(1.3, second.value(TrimAbility.SENTRY, "damage-multiplier"));
        assertThrows(IllegalArgumentException.class,
                () -> second.setValue(TrimAbility.DUNE, "knockback-resistance", 2));
        assertEquals(.4, second.value(TrimAbility.DUNE, "knockback-resistance"));
    }

    @Test
    void upgradesVersionTenWithEyeDefaults() throws Exception {
        Files.writeString(directory.resolve("settings.yml"), """
                schema-version: 10
                abilities:
                  eye:
                    enabled: true
                """);
        SettingsManager settings = new SettingsManager(directory, Logger.getAnonymousLogger());
        settings.load();
        assertEquals(1, settings.intValue(TrimAbility.EYE, "weakness-level"));
        assertEquals(1, settings.intValue(TrimAbility.EYE, "slowness-level"));
        assertEquals(1, settings.intValue(TrimAbility.EYE, "glowing-enabled"));
        String saved = Files.readString(directory.resolve("settings.yml"));
        assertTrue(saved.contains("schema-version: 12"));
        assertTrue(saved.contains("weakness-level: 1.0"));
    }

    @Test
    void upgradesVexFireResistanceSettingAndPersistsEdits() throws Exception {
        Files.writeString(directory.resolve("settings.yml"), """
                schema-version: 11
                abilities:
                  vex:
                    values:
                      duration-seconds: 45
                """);
        SettingsManager settings = new SettingsManager(directory, Logger.getAnonymousLogger());
        settings.load();
        assertEquals(45, settings.value(TrimAbility.VEX, "duration-seconds"));
        assertEquals(90, settings.value(TrimAbility.VEX, "fire-resistance-duration-seconds"));
        assertTrue(Files.readString(directory.resolve("settings.yml"))
                .contains("fire-resistance-duration-seconds: 90.0"));

        settings.setValue(TrimAbility.VEX, "fire-resistance-duration-seconds", 55);
        SettingsManager reloaded = new SettingsManager(directory, Logger.getAnonymousLogger());
        reloaded.load();
        assertEquals(55, reloaded.value(TrimAbility.VEX, "fire-resistance-duration-seconds"));
        assertEquals(45, reloaded.value(TrimAbility.VEX, "duration-seconds"));
        reloaded.reset(TrimAbility.VEX);
        assertEquals(90, reloaded.value(TrimAbility.VEX, "fire-resistance-duration-seconds"));
    }

    @Test
    void migratesOnlyOldVersionOneDefaults() throws Exception {
        Files.writeString(directory.resolve("settings.yml"), """
                schema-version: 1
                abilities:
                  bolt:
                    values:
                      lightning-damage: 4
                  vex:
                    values:
                      duration-seconds: 15
                """);
        SettingsManager migrated = new SettingsManager(directory, Logger.getAnonymousLogger());
        migrated.load();
        assertEquals(10, migrated.value(TrimAbility.BOLT, "bonus-damage"));
        assertEquals(90, migrated.value(TrimAbility.VEX, "duration-seconds"));
        assertTrue(Files.readString(directory.resolve("settings.yml")).contains("schema-version: 12"));

        Path custom = Files.createDirectory(directory.resolve("custom"));
        Files.writeString(custom.resolve("settings.yml"), """
                schema-version: 1
                abilities:
                  bolt:
                    values:
                      lightning-damage: 7
                  vex:
                    values:
                      duration-seconds: 45
                """);
        SettingsManager preserved = new SettingsManager(custom, Logger.getAnonymousLogger());
        preserved.load();
        assertEquals(7, preserved.value(TrimAbility.BOLT, "bonus-damage"));
        assertEquals(45, preserved.value(TrimAbility.VEX, "duration-seconds"));
    }

    @Test
    void removesObsoleteEyeSettingsAndCoastLuck() throws Exception {
        Files.writeString(directory.resolve("settings.yml"), """
                schema-version: 3
                abilities:
                  coast:
                    values:
                      luck-level: 4
                  eye:
                    values:
                      radius: 22
                      glow-seconds: 25
                  host:
                    values:
                      hero-level: 3
                """);
        SettingsManager settings = new SettingsManager(directory, Logger.getAnonymousLogger());
        settings.load();
        assertEquals(1, settings.intValue(TrimAbility.EYE, "weakness-level"));
        assertEquals(3, settings.value(TrimAbility.HOST, "hero-level"));
        String saved = Files.readString(directory.resolve("settings.yml"));
        assertFalse(saved.contains("glow-seconds"));
        assertFalse(saved.contains("radius: 22"));
        assertFalse(saved.contains("reveal-seconds"));
        assertFalse(saved.contains("luck-level: 4"));
        assertTrue(saved.contains("schema-version: 12"));
    }

    @Test
    void oldDefaultEyeGlowIsRemoved() throws Exception {
        Files.writeString(directory.resolve("settings.yml"), """
                schema-version: 3
                abilities:
                  eye:
                    values:
                      glow-seconds: 15
                """);
        SettingsManager settings = new SettingsManager(directory, Logger.getAnonymousLogger());
        settings.load();
        assertFalse(Files.readString(directory.resolve("settings.yml")).contains("glow-seconds"));
        assertEquals(1, settings.intValue(TrimAbility.EYE, "glowing-enabled"));
    }

    @Test
    void mergesNautilusSettingsUsingRegularValue() throws Exception {
        Files.writeString(directory.resolve("settings.yml"), """
                schema-version: 4
                abilities:
                  coast:
                    values:
                      nautilus-multiplier: 1.8
                      zombie-nautilus-multiplier: 2.6
                """);
        SettingsManager settings = new SettingsManager(directory, Logger.getAnonymousLogger());
        settings.load();
        assertEquals(1.8, settings.value(TrimAbility.COAST, "nautilus-multiplier"));
        String saved = Files.readString(directory.resolve("settings.yml"));
        assertFalse(saved.contains("zombie-nautilus-multiplier"));
        assertTrue(saved.contains("schema-version: 12"));
    }

    @Test
    void removesVehicleSettingsWithoutChangingLivingMountSettings() throws Exception {
        Files.writeString(directory.resolve("settings.yml"), """
                schema-version: 5
                abilities:
                  coast:
                    values:
                      land-multiplier: 1.7
                      nautilus-multiplier: 1.8
                      happy-ghast-multiplier: 1.9
                      boat-multiplier: 2.0
                      minecart-multiplier: 2.1
                """);
        SettingsManager settings = new SettingsManager(directory, Logger.getAnonymousLogger());
        settings.load();
        assertEquals(1.7, settings.value(TrimAbility.COAST, "land-multiplier"));
        assertEquals(1.8, settings.value(TrimAbility.COAST, "nautilus-multiplier"));
        assertEquals(1.9, settings.value(TrimAbility.COAST, "happy-ghast-multiplier"));
        String saved = Files.readString(directory.resolve("settings.yml"));
        assertFalse(saved.contains("boat-multiplier"));
        assertFalse(saved.contains("minecart-multiplier"));
        assertTrue(saved.contains("schema-version: 12"));
    }

    @Test
    void migratesBoltDefaultAndKeepsCustomBonus() throws Exception {
        Files.writeString(directory.resolve("settings.yml"), """
                schema-version: 6
                abilities:
                  bolt:
                    values:
                      lightning-damage: 16
                """);
        SettingsManager defaults = new SettingsManager(directory, Logger.getAnonymousLogger());
        defaults.load();
        assertEquals(10, defaults.value(TrimAbility.BOLT, "bonus-damage"));
        String saved = Files.readString(directory.resolve("settings.yml"));
        assertFalse(saved.contains("lightning-damage"));
        assertTrue(saved.contains("bonus-damage: 10"));
        assertTrue(saved.contains("schema-version: 12"));

        Path custom = Files.createDirectory(directory.resolve("custom-bolt"));
        Files.writeString(custom.resolve("settings.yml"), """
                schema-version: 6
                abilities:
                  bolt:
                    values:
                      lightning-damage: 7
                """);
        SettingsManager customized = new SettingsManager(custom, Logger.getAnonymousLogger());
        customized.load();
        assertEquals(7, customized.value(TrimAbility.BOLT, "bonus-damage"));
        assertFalse(Files.readString(custom.resolve("settings.yml")).contains("lightning-damage"));
    }
}
