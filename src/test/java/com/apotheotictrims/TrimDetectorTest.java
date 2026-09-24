package com.apotheotictrims;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TrimDetectorTest {
    @Test
    void acceptsFourMatchingPatterns() {
        assertEquals("tide", TrimDetector.commonPattern(List.of("tide", "tide", "tide", "tide")).orElseThrow());
    }

    @Test
    void rejectsMissingUntrimmedAndMixedSets() {
        assertTrue(TrimDetector.commonPattern(List.of("tide", "tide", "tide")).isEmpty());
        assertTrue(TrimDetector.commonPattern(Arrays.asList("tide", null, "tide", "tide")).isEmpty());
        assertTrue(TrimDetector.commonPattern(List.of("tide", "tide", "ward", "tide")).isEmpty());
    }
}
