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
        when(effects.refresh(any(), any(), anyInt())).thenReturn(true);
        EyeStareManager manager = manager(effects);

        manager.update(viewer, target, true);
        verify(effects).refresh(target, WEAKNESS, 1);
        verify(effects).refresh(target, GLOWING, 1);
        verify(effects, never()).refresh(eq(target), eq(SLOWNESS), anyInt());
        verify(world).spawnParticle(eq(Particle.GLOW), any(Location.class), eq(2),
                eq(.25), eq(.45), eq(.25), eq(0.0));
        verify(viewer, never()).spawnParticle(any(), any(Location.class), anyInt());

        manager.update(viewer, null, true);
        verify(effects).clear(target, WEAKNESS, 1);
        verify(effects).clear(target, GLOWING, 1);
    }

    @Test
    void sneakAddsSlownessAndReleasingSneakClearsOnlySlowness() {
        World world = mock(World.class);
        Player viewer = player(world), target = player(world);
        when(viewer.isSneaking()).thenReturn(true, false);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        when(effects.refresh(any(), any(), anyInt())).thenReturn(true);
        EyeStareManager manager = manager(effects);

        manager.update(viewer, target, false);
        verify(effects).refresh(target, SLOWNESS, 1);
        manager.update(viewer, target, false);
        verify(effects).clear(target, SLOWNESS, 1);
        verify(effects, never()).clear(eq(target), eq(WEAKNESS), anyInt());
        verify(effects, never()).clear(eq(target), eq(GLOWING), anyInt());
    }

    @Test
    void multipleViewersKeepEffectsUntilLastLeavesAndSneakIsShared() {
        World world = mock(World.class);
        Player first = player(world), second = player(world), target = player(world);
        when(first.isSneaking()).thenReturn(true);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        when(effects.refresh(any(), any(), anyInt())).thenReturn(true);
        EyeStareManager manager = manager(effects);

        manager.update(first, target, false);
        manager.update(second, target, false);
        manager.clear(second);
        verify(effects, never()).clear(eq(target), eq(SLOWNESS), anyInt());
        manager.clear(first);
        verify(effects).clear(target, SLOWNESS, 1);
        verify(effects).clear(target, WEAKNESS, 1);
        verify(effects).clear(target, GLOWING, 1);
    }

    @Test
    void switchingTargetsAndTargetCleanupReleaseEffects() {
        World world = mock(World.class);
        Player viewer = player(world), first = player(world), second = player(world);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        when(effects.refresh(any(), any(), anyInt())).thenReturn(true);
        EyeStareManager manager = manager(effects);

        manager.update(viewer, first, false);
        manager.update(viewer, second, false);
        verify(effects).clear(first, WEAKNESS, 1);
        manager.clear(second);
        verify(effects).clear(second, WEAKNESS, 1);
    }

    @Test
    void leavesPreexistingStrongerEffectsUntouched() {
        World world = mock(World.class);
        Player viewer = player(world), target = player(world);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        EyeStareManager manager = manager(effects);

        manager.update(viewer, target, false);
        manager.clear(viewer);
        verify(effects, never()).clear(any(), any(), anyInt());
    }

    @Test
    void nonWearerTargetIsNotClearedByViewerCleanup() {
        World world = mock(World.class);
        Player viewer = player(world), target = player(world);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        when(effects.refresh(any(), any(), anyInt())).thenReturn(true);
        EyeStareManager manager = manager(effects);

        manager.update(viewer, target, false);
        manager.clearViewer(target);
        verify(effects, never()).clear(any(), any(), anyInt());
        manager.clearViewer(viewer);
        verify(effects).clear(target, WEAKNESS, 1);
        verify(effects).clear(target, GLOWING, 1);
    }

    @Test
    void usesConfiguredLevelsAndCanDisableGlowingDuringStare() {
        World world = mock(World.class);
        Player viewer = player(world), target = player(world);
        when(viewer.isSneaking()).thenReturn(true);
        SettingsManager settings = mock(SettingsManager.class);
        when(settings.intValue(TrimAbility.EYE, "weakness-level")).thenReturn(3);
        when(settings.intValue(TrimAbility.EYE, "slowness-level")).thenReturn(2);
        when(settings.intValue(TrimAbility.EYE, "glowing-enabled")).thenReturn(1, 0);
        EyeStareManager.EyeEffects effects = mock(EyeStareManager.EyeEffects.class);
        when(effects.refresh(any(), any(), anyInt())).thenReturn(true);
        EyeStareManager manager = new EyeStareManager(settings, effects);

        manager.update(viewer, target, false);
        verify(effects).refresh(target, WEAKNESS, 3);
        verify(effects).refresh(target, SLOWNESS, 2);
        verify(effects).refresh(target, GLOWING, 1);
        manager.update(viewer, target, false);
        verify(effects).clear(target, GLOWING, 1);
        verify(effects, never()).clear(eq(target), eq(WEAKNESS), anyInt());
    }

    private EyeStareManager manager(EyeStareManager.EyeEffects effects) {
        SettingsManager settings = mock(SettingsManager.class);
        when(settings.intValue(TrimAbility.EYE, "weakness-level")).thenReturn(1);
        when(settings.intValue(TrimAbility.EYE, "slowness-level")).thenReturn(1);
        when(settings.intValue(TrimAbility.EYE, "glowing-enabled")).thenReturn(1);
        return new EyeStareManager(settings, effects);
    }

    private Player player(World world) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        return player;
    }
}
