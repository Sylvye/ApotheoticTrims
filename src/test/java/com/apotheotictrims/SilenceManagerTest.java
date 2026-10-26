package com.apotheotictrims;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SilenceManagerTest {
    private final SettingsManager settings = mock(SettingsManager.class);
    private final SilenceManager.Visibility visibility = mock(SilenceManager.Visibility.class);
    private final Player player = player(), viewer = player();
    private final World world = mock(World.class);
    private final SilenceManager manager = new SilenceManager(settings, visibility);
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach void setup() {
        when(settings.value(TrimAbility.SILENCE, "charge-seconds")).thenReturn(15.0);
        when(player.isSneaking()).thenReturn(true);
        when(player.getWorld()).thenReturn(world);
        when(player.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getOnlinePlayers).thenReturn(List.of(player, viewer));
    }

    @AfterEach void teardown() { bukkit.close(); }

    @Test void chargesForExactlyThreeHundredTicksAndStaysHidden() {
        ticks(299);
        verify(visibility, never()).start(any(), anyLong());
        ticks(1);
        verify(visibility).start(player, 300);
        verify(visibility, atLeastOnce()).equipment(viewer, player, true);
        verify(visibility, never()).equipment(player, player, true);
        ticks(50);
        verify(visibility, times(1)).start(any(), anyLong());
        verify(visibility, never()).stop(any(), anyLong());
    }

    @Test void releaseDiscardsPartialChargeAndRestoresHiddenEquipment() {
        ticks(150);
        when(player.isSneaking()).thenReturn(false);
        ticks(1);
        when(player.isSneaking()).thenReturn(true);
        ticks(299);
        verify(visibility, never()).start(any(), anyLong());
        ticks(1);
        when(player.isSneaking()).thenReturn(false);
        ticks(1);
        verify(visibility).stop(eq(player), anyLong());
        verify(visibility).equipment(viewer, player, false);
        manager.mask(viewer, player);
        verify(visibility, times(1)).stop(any(), anyLong());
    }

    @Test void damageResetAutomaticallyRechargesWhileSneakRemainsHeld() {
        ticks(300);
        manager.clear(player);
        ticks(299);
        verify(visibility, times(1)).start(any(), anyLong());
        ticks(1);
        verify(visibility, times(2)).start(any(), anyLong());
    }

    @Test void eligibilityLossDeathAndShutdownRestoreVisibility() {
        ticks(300);
        manager.tick(List.of(player), ignored -> false);
        verify(visibility, times(1)).stop(any(), anyLong());
        ticks(300);
        when(player.isDead()).thenReturn(true);
        ticks(1);
        verify(visibility, times(2)).stop(any(), anyLong());
        when(player.isDead()).thenReturn(false);
        ticks(300);
        manager.clear();
        manager.clear();
        verify(visibility, times(3)).stop(any(), anyLong());
    }

    @Test void configuredChargeFeedbackStopsParticlesAtActivation() {
        when(settings.value(TrimAbility.SILENCE, "charge-seconds")).thenReturn(.5);
        when(settings.particles()).thenReturn(true);
        when(settings.actionBar()).thenReturn(true);
        ticks(9);
        verify(player, atLeastOnce()).sendActionBar(any(Component.class));
        verify(world, atLeastOnce()).spawnParticle(eq(Particle.SMOKE), any(Location.class), eq(2),
                eq(.25), eq(.4), eq(.25), eq(.005));
        clearInvocations(world);
        ticks(50);
        verifyNoInteractions(world);
        verify(visibility).start(player, 10);
    }

    @Test void disabledFeedbackDoesNotBlockChargingOrMaskingNewViewers() {
        ticks(300);
        verify(player, never()).sendActionBar(any(Component.class));
        verifyNoInteractions(world);
        Player newcomer = player();
        manager.mask(newcomer, player);
        verify(visibility).equipment(newcomer, player, true);
        manager.clear(player);
        clearInvocations(visibility);
        manager.mask(newcomer, player);
        verifyNoInteractions(visibility);
    }

    private void ticks(int count) {
        for (int i = 0; i < count; i++) manager.tick(List.of(player), ignored -> true);
    }

    private static Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }
}
