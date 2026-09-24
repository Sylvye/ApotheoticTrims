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
    void persistsValuesTogglesFeedbackAndResets() {
        SettingsManager first = new SettingsManager(directory, Logger.getAnonymousLogger());
        first.load();
        first.setValue(TrimAbility.BOLT, "combo-hits", 35);
        first.setEnabled(TrimAbility.WARD, false);
        first.setFeedback("particles", false);

        SettingsManager second = new SettingsManager(directory, Logger.getAnonymousLogger());
        second.load();
        assertEquals(35, second.intValue(TrimAbility.BOLT, "combo-hits"));
        assertFalse(second.enabled(TrimAbility.WARD));
        assertFalse(second.particles());

        second.reset(TrimAbility.BOLT);
        assertEquals(10, second.intValue(TrimAbility.BOLT, "combo-hits"));
        assertEquals(16, second.value(TrimAbility.BOLT, "lightning-damage"));
        assertEquals(90, second.value(TrimAbility.VEX, "duration-seconds"));
        assertEquals(2, second.intValue(TrimAbility.WAYFINDER, "speed-level"));
        assertEquals(2, second.intValue(TrimAbility.SHAPER, "haste-level"));
        assertEquals(5, second.intValue(TrimAbility.HOST, "hero-level"));
        assertEquals(1.3, second.value(TrimAbility.SENTRY, "damage-multiplier"));
        assertThrows(IllegalArgumentException.class,
                () -> second.setValue(TrimAbility.DUNE, "knockback-resistance", 2));
        assertEquals(.4, second.value(TrimAbility.DUNE, "knockback-resistance"));
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
        assertEquals(16, migrated.value(TrimAbility.BOLT, "lightning-damage"));
        assertEquals(90, migrated.value(TrimAbility.VEX, "duration-seconds"));
        assertTrue(Files.readString(directory.resolve("settings.yml")).contains("schema-version: 3"));

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
        assertEquals(7, preserved.value(TrimAbility.BOLT, "lightning-damage"));
        assertEquals(45, preserved.value(TrimAbility.VEX, "duration-seconds"));
    }
}
