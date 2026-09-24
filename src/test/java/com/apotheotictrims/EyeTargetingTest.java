package com.apotheotictrims;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EyeTargetingTest {
    @Test
    void findsDistantVisibleTargetButRejectsObstructedOne() {
        World world = mock(World.class);
        Player viewer = mock(Player.class), target = mock(Player.class);
        when(viewer.getWorld()).thenReturn(world);
        when(world.getPlayers()).thenReturn(List.of(viewer, target));
        when(viewer.getEyeLocation()).thenReturn(new Location(world, 0, 65, 0, 0, 0));
        when(target.getEyeLocation()).thenReturn(new Location(world, 0, 65, 300));
        when(target.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(viewer.canSee(target)).thenReturn(true);
        when(viewer.hasLineOfSight(target)).thenReturn(true);
        assertSame(target, EyeTargeting.findTarget(viewer));
        when(viewer.hasLineOfSight(target)).thenReturn(false);
        assertNull(EyeTargeting.findTarget(viewer));
    }
}
