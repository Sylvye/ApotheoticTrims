package com.apotheotictrims;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.apotheotictrims.EyeStareManager.EyeEffect.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EyeStareManagerTest {
    @Test
    void appliesBaseEffectsAndPublicParticlesThenClearsOnLostTarget() {
        World world = mock(World.class);
        Player viewer = player(world), target = player(world);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        when(effects.refresh(any(), any())).thenReturn(true);
        EyeStareManager manager = new EyeStareManager(effects);

        manager.update(viewer, target, true);
        verify(effects).refresh(target, WEAKNESS);
        verify(effects).refresh(target, GLOWING);
        verify(effects, never()).refresh(target, SLOWNESS);
        verify(world).spawnParticle(eq(Particle.GLOW), any(Location.class), eq(2),
                eq(.25), eq(.45), eq(.25), eq(0.0));
        verify(viewer, never()).spawnParticle(any(), any(Location.class), anyInt());

        manager.update(viewer, null, true);
        verify(effects).clear(target, WEAKNESS);
        verify(effects).clear(target, GLOWING);
    }

    @Test
    void sneakAddsSlownessAndReleasingSneakClearsOnlySlowness() {
        World world = mock(World.class);
        Player viewer = player(world), target = player(world);
        when(viewer.isSneaking()).thenReturn(true, false);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        when(effects.refresh(any(), any())).thenReturn(true);
        EyeStareManager manager = new EyeStareManager(effects);

        manager.update(viewer, target, false);
        verify(effects).refresh(target, SLOWNESS);
        manager.update(viewer, target, false);
        verify(effects).clear(target, SLOWNESS);
        verify(effects, never()).clear(target, WEAKNESS);
        verify(effects, never()).clear(target, GLOWING);
    }

    @Test
    void multipleViewersKeepEffectsUntilLastLeavesAndSneakIsShared() {
        World world = mock(World.class);
        Player first = player(world), second = player(world), target = player(world);
        when(first.isSneaking()).thenReturn(true);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        when(effects.refresh(any(), any())).thenReturn(true);
        EyeStareManager manager = new EyeStareManager(effects);

        manager.update(first, target, false);
        manager.update(second, target, false);
        manager.clear(second);
        verify(effects, never()).clear(target, SLOWNESS);
        manager.clear(first);
        verify(effects).clear(target, SLOWNESS);
        verify(effects).clear(target, WEAKNESS);
        verify(effects).clear(target, GLOWING);
    }

    @Test
    void switchingTargetsAndTargetCleanupReleaseEffects() {
        World world = mock(World.class);
        Player viewer = player(world), first = player(world), second = player(world);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        when(effects.refresh(any(), any())).thenReturn(true);
        EyeStareManager manager = new EyeStareManager(effects);

        manager.update(viewer, first, false);
        manager.update(viewer, second, false);
        verify(effects).clear(first, WEAKNESS);
        manager.clear(second);
        verify(effects).clear(second, WEAKNESS);
    }

    @Test
    void leavesPreexistingStrongerEffectsUntouched() {
        World world = mock(World.class);
        Player viewer = player(world), target = player(world);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        EyeStareManager manager = new EyeStareManager(effects);

        manager.update(viewer, target, false);
        manager.clear(viewer);
        verify(effects, never()).clear(any(), any());
    }

    @Test
    void nonWearerTargetIsNotClearedByViewerCleanup() {
        World world = mock(World.class);
        Player viewer = player(world), target = player(world);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        when(effects.refresh(any(), any())).thenReturn(true);
        EyeStareManager manager = new EyeStareManager(effects);

        manager.update(viewer, target, false);
        manager.clearViewer(target);
        verify(effects, never()).clear(any(), any());
        manager.clearViewer(viewer);
        verify(effects).clear(target, WEAKNESS);
        verify(effects).clear(target, GLOWING);
    }

    private Player player(World world) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        return player;
    }
}
