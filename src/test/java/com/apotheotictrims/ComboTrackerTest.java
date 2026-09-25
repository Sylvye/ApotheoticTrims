package com.apotheotictrims;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ComboTrackerTest {
    @Test
    void buildsTriggersAndResetsACombo() {
        ComboTracker tracker = new ComboTracker();
        UUID player = UUID.randomUUID();
        assertEquals(1, tracker.hit(player, 1000, 0, 3, 5000).count());
        assertFalse(tracker.hit(player, 2000, 10, 3, 5000).triggered());
        assertTrue(tracker.hit(player, 3000, 20, 3, 5000).triggered());
        assertEquals(0, tracker.count(player));
        assertEquals(0, tracker.hit(player, 3100, 21, 3, 5000).count());
        assertEquals(1, tracker.hit(player, 3500, 30, 3, 5000).count());
    }

    @Test
    void timeoutStartsAtOneAgain() {
        ComboTracker tracker = new ComboTracker();
        UUID player = UUID.randomUUID();
        tracker.hit(player, 1000, 0, 20, 5000);
        assertEquals(1, tracker.hit(player, 7001, 10, 20, 5000).count());
    }

    @Test
    void rapidHitsDoNotAdvanceOrExtendCombo() {
        ComboTracker tracker = new ComboTracker();
        UUID player = UUID.randomUUID();
        assertEquals(1, tracker.hit(player, 1000, 100, 3, 5000).count());
        assertEquals(1, tracker.hit(player, 2000, 109, 3, 5000).count());
        assertEquals(2, tracker.hit(player, 3000, 110, 3, 5000).count());
        assertEquals(2, tracker.hit(player, 8001, 119, 3, 5000).count());
        assertEquals(1, tracker.hit(player, 8002, 120, 3, 5000).count());
    }
}
