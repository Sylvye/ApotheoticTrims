package com.apotheotictrims;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingSpecTest {
    @Test
    void validatesRangeAndIntegralValues() {
        SettingSpec spec = new SettingSpec("level", "Level", 1, 1, 10, 1, 2, true);
        assertEquals(5, spec.validate(5));
        assertThrows(IllegalArgumentException.class, () -> spec.validate(0));
        assertThrows(IllegalArgumentException.class, () -> spec.validate(2.5));
        assertThrows(IllegalArgumentException.class, () -> spec.validate(Double.NaN));
    }

    @Test
    void clampsGuiAdjustments() {
        SettingSpec spec = new SettingSpec("radius", "Radius", 6, 0, 64, .5, 2, false);
        assertEquals(64, spec.clamp(100));
        assertEquals(0, spec.clamp(-1));
        assertEquals("6.5", spec.format(6.5));
    }
}
