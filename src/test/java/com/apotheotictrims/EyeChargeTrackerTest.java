package com.apotheotictrims;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EyeChargeTrackerTest {
    @Test
    void chargesEachContinuousStareOnceAndResetsOnTargetChange() {
        EyeChargeTracker tracker = new EyeChargeTracker();
        UUID viewer = UUID.randomUUID(), first = UUID.randomUUID(), second = UUID.randomUUID();
        assertFalse(tracker.stare(viewer, first, 0, 100));
        assertFalse(tracker.stare(viewer, first, 90, 100));
        assertTrue(tracker.stare(viewer, first, 100, 100));
        assertFalse(tracker.stare(viewer, first, 200, 100));
        assertFalse(tracker.stare(viewer, second, 210, 100));
        assertTrue(tracker.stare(viewer, second, 310, 100));
        assertFalse(tracker.stare(viewer, null, 320, 100));
        assertFalse(tracker.stare(viewer, first, 330, 100));
        assertTrue(tracker.stare(viewer, first, 430, 100));
    }

    @Test
    void sneakRevealTriggersOnceUntilReleased() {
        EyeChargeTracker tracker = new EyeChargeTracker();
        UUID viewer = UUID.randomUUID();
        assertFalse(tracker.sneak(viewer, true, 0, 100));
        assertTrue(tracker.sneak(viewer, true, 100, 100));
        assertFalse(tracker.sneak(viewer, true, 200, 100));
        assertFalse(tracker.sneak(viewer, false, 210, 100));
        assertFalse(tracker.sneak(viewer, true, 220, 100));
        assertTrue(tracker.sneak(viewer, true, 320, 100));
        tracker.clear(viewer);
        assertFalse(tracker.sneak(viewer, true, 330, 100));
    }
}
