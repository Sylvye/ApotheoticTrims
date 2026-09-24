package com.apotheotictrims;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class LandingProtectionTrackerTest {
    @Test
    void consumesExactlyOneProtectedLanding() {
        LandingProtectionTracker tracker = new LandingProtectionTracker();
        UUID player = UUID.randomUUID();
        tracker.arm(player, 100);
        assertTrue(tracker.consume(player));
        assertFalse(tracker.consume(player));
    }

    @Test
    void clearsSafeLandingsButNotTheLaunchTick() {
        LandingProtectionTracker tracker = new LandingProtectionTracker();
        UUID player = UUID.randomUUID();
        tracker.arm(player, 100);
        assertFalse(tracker.clearAfterSafeLanding(player, 101, true, false));
        assertTrue(tracker.clearAfterSafeLanding(player, 103, true, false));
        assertFalse(tracker.consume(player));

        tracker.arm(player, 200);
        assertTrue(tracker.clearAfterSafeLanding(player, 203, false, true));
    }
}
