package com.apotheotictrims;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CoastMountManagerTest {
    @TempDir Path directory;

    @Test
    void classifiesNamedAndOtherLivingMounts() {
        assertEquals(CoastMountCategory.LAND, CoastMountCategory.of(mock(Horse.class)));
        assertEquals(CoastMountCategory.LAND, CoastMountCategory.of(mock(CamelHusk.class)));
        assertEquals(CoastMountCategory.LAND, CoastMountCategory.of(mock(Pig.class)));
        assertEquals(CoastMountCategory.LAND, CoastMountCategory.of(mock(Strider.class)));
        assertEquals(CoastMountCategory.LAND, CoastMountCategory.of(mock(Cow.class)));
        assertEquals(CoastMountCategory.NAUTILUS, CoastMountCategory.of(mock(Nautilus.class)));
        assertEquals(CoastMountCategory.NAUTILUS, CoastMountCategory.of(mock(ZombieNautilus.class)));
        assertEquals(CoastMountCategory.HAPPY_GHAST, CoastMountCategory.of(mock(HappyGhast.class)));
        assertNull(CoastMountCategory.of(mock(Boat.class)));
        assertNull(CoastMountCategory.of(mock(ChestBoat.class)));
        assertNull(CoastMountCategory.of(mock(Minecart.class)));
    }

    @Test
    void sharedGhastCollectsOneBuffUntilLastWearerLeaves() {
        SettingsManager settings = new SettingsManager(directory, Logger.getAnonymousLogger());
        CoastMountManager manager = new CoastMountManager(settings,
                new NamespacedKey("apotheotictrims", "coast_mount_speed"));
        HappyGhast ghast = mock(HappyGhast.class);
        when(ghast.getUniqueId()).thenReturn(UUID.randomUUID());
        when(ghast.isValid()).thenReturn(true);
        Player first = mock(Player.class), second = mock(Player.class);
        when(first.getVehicle()).thenReturn(ghast);
        when(second.getVehicle()).thenReturn(ghast);
        assertEquals(1, manager.collect(List.of(first, second), player -> true).size());
        assertEquals(1.3, manager.collect(List.of(first, second), player -> true)
                .get(ghast.getUniqueId()).multiplier());
        assertEquals(1, manager.collect(List.of(second), player -> true).size());
        assertTrue(manager.collect(List.of(), player -> true).isEmpty());
    }

}
