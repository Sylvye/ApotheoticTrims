package com.apotheotictrims;

import org.bukkit.FluidCollisionMode;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EyeTargetingTest {
    @Test
    void torsoHitCountsAndBlocksPreventTargeting() {
        World world = mock(World.class);
        Player viewer = viewer(world), target = candidate(world, 0, 10);
        when(world.getPlayers()).thenReturn(List.of(viewer, target));
        assertSame(target, EyeTargeting.findTarget(viewer));
        when(world.rayTraceBlocks(any(Location.class), any(Vector.class), anyDouble(),
                eq(FluidCollisionMode.NEVER), eq(true)))
                .thenReturn(new RayTraceResult(new Vector(0, 65, 5)));
        assertNull(EyeTargeting.findTarget(viewer));
    }

    @Test
    void closeAimLeewayTapersAwayAtLongRange() {
        World world = mock(World.class);
        Player viewer = viewer(world), close = candidate(world, .38, 5), distant = candidate(world, .38, 100);
        when(world.getPlayers()).thenReturn(List.of(viewer, close));
        assertSame(close, EyeTargeting.findTarget(viewer));
        when(world.getPlayers()).thenReturn(List.of(viewer, distant));
        assertNull(EyeTargeting.findTarget(viewer));
    }

    @Test
    void nearestHitboxWins() {
        World world = mock(World.class);
        Player viewer = viewer(world), far = candidate(world, 0, 20), near = candidate(world, 0, 5);
        when(world.getPlayers()).thenReturn(List.of(viewer, far, near));
        assertSame(near, EyeTargeting.findTarget(viewer));
    }

    private Player viewer(World world) {
        Player player = mock(Player.class);
        when(player.getWorld()).thenReturn(world);
        when(player.getEyeLocation()).thenReturn(new Location(world, 0, 65, 0, 0, 0));
        when(player.canSee(any(Player.class))).thenReturn(true);
        return player;
    }

    private Player candidate(World world, double x, double z) {
        Player player = mock(Player.class);
        when(player.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(player.getBoundingBox()).thenReturn(BoundingBox.of(new Vector(x, 64.9, z), .3, .9, .3));
        return player;
    }
}
