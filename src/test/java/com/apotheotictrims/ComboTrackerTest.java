package com.apotheotictrims;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ComboTrackerTest {
    @Test
    void buildsTriggersAndResetsACombo() {
        ComboTracker tracker = new ComboTracker();
        UUID player = UUID.randomUUID();
        assertEquals(1, tracker.hit(player, 1000, 3, 5000).count());
        assertFalse(tracker.hit(player, 2000, 3, 5000).triggered());
        assertTrue(tracker.hit(player, 3000, 3, 5000).triggered());
        assertEquals(0, tracker.count(player));
    }

    @Test
    void timeoutStartsAtOneAgain() {
        ComboTracker tracker = new ComboTracker();
        UUID player = UUID.randomUUID();
        tracker.hit(player, 1000, 20, 5000);
        assertEquals(1, tracker.hit(player, 7001, 20, 5000).count());
    }
}
