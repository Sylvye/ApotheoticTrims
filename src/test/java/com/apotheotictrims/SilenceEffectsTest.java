package com.apotheotictrims;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SilenceEffectsTest {
    @Test
    void blocksExactlyTheSpecifiedEffects() {
        assertEquals(14, SilenceEffects.BLOCKED_KEYS.size());
        assertTrue(SilenceEffects.BLOCKED_KEYS.contains("instant_damage"));
        assertTrue(SilenceEffects.BLOCKED_KEYS.contains("slow_falling"));
        assertTrue(SilenceEffects.BLOCKED_KEYS.contains("unluck"));
        assertFalse(SilenceEffects.BLOCKED_KEYS.contains("bad_omen"));
        assertFalse(SilenceEffects.BLOCKED_KEYS.contains("regeneration"));
    }
}
