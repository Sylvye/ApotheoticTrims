package com.apotheotictrims;

import io.papermc.paper.event.player.PlayerShieldDisableEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlowBlastTest {
    private final ApotheoticTrimsPlugin plugin = mock(ApotheoticTrimsPlugin.class);
    private final SettingsManager settings = mock(SettingsManager.class);
    private final AbilityManager abilities = mock(AbilityManager.class);
    private final Player wearer = mock(Player.class);
    private final World world = mock(World.class);
    private final BukkitScheduler scheduler = mock(BukkitScheduler.class);
    private final PlayerShieldDisableEvent event = mock(PlayerShieldDisableEvent.class);
    private final AbilityListener listener = new AbilityListener(plugin, settings, abilities);
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setup() {
        when(event.getPlayer()).thenReturn(wearer);
        when(abilities.has(wearer, TrimAbility.FLOW)).thenReturn(true);
        when(wearer.getWorld()).thenReturn(world);
        when(wearer.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        when(settings.value(TrimAbility.FLOW, "radius")).thenReturn(6.0);
        when(settings.value(TrimAbility.FLOW, "horizontal-velocity")).thenReturn(2.0);
        when(settings.value(TrimAbility.FLOW, "vertical-velocity")).thenReturn(.7);
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
            call.<Runnable>getArgument(1).run();
            return mock(BukkitTask.class);
        });
    }

    @AfterEach
    void cleanup() {
        bukkit.close();
    }

    @Test
    void blastKnocksBackPlayersAndMobsButExcludesWearerAndSpectators() {
        Player target = mock(Player.class);
        when(target.getGameMode()).thenReturn(GameMode.SURVIVAL);
        when(target.getLocation()).thenReturn(new Location(world, 3, 64, 0));
        Player spectator = mock(Player.class);
        when(spectator.getGameMode()).thenReturn(GameMode.SPECTATOR);
        LivingEntity mob = mock(LivingEntity.class);
        when(mob.getLocation()).thenReturn(new Location(world, 0, 64, -3));
        when(world.getNearbyLivingEntities(wearer.getLocation(), 6.0))
                .thenReturn(List.of(wearer, target, spectator, mob));

        listener.onShieldDisabled(event);

        verify(target).setVelocity(new Vector(2, .7, 0));
        verify(mob).setVelocity(new Vector(0, .7, -2));
        verify(target).setCooldown(Material.WIND_CHARGE, 100);
        verify(target).setCooldown(Material.ENDER_PEARL, 100);
        verify(wearer, never()).setVelocity(any());
        verify(wearer, never()).setCooldown(any(Material.class), anyInt());
        verify(spectator, never()).setVelocity(any());
        verify(spectator, never()).setCooldown(any(Material.class), anyInt());
        verify(abilities).flowTriggerFeedback(wearer);
    }

    @Test
    void repeatedBlastsRefreshShorterCooldownsAndPreserveLongerOnes() {
        Player target = mock(Player.class);
        when(target.getLocation()).thenReturn(new Location(world, 3, 64, 0));
        when(world.getNearbyLivingEntities(wearer.getLocation(), 6.0)).thenReturn(List.of(target));
        when(target.getCooldown(Material.WIND_CHARGE)).thenReturn(0, 60, 160);
        when(target.getCooldown(Material.ENDER_PEARL)).thenReturn(180, 80, 100);

        listener.onShieldDisabled(event);
        listener.onShieldDisabled(event);
        listener.onShieldDisabled(event);

        verify(target, times(2)).setCooldown(Material.WIND_CHARGE, 100);
        verify(target).setCooldown(Material.WIND_CHARGE, 160);
        verify(target).setCooldown(Material.ENDER_PEARL, 180);
        verify(target, times(2)).setCooldown(Material.ENDER_PEARL, 100);
    }

    @Test
    void cancelledDisableDoesNotBlast() {
        when(event.isCancelled()).thenReturn(true);

        listener.onShieldDisabled(event);

        verifyNoInteractions(scheduler, abilities, world);
    }

    @Test
    void inactiveFlowDoesNotBlast() {
        when(abilities.has(wearer, TrimAbility.FLOW)).thenReturn(false);

        listener.onShieldDisabled(event);

        verifyNoInteractions(scheduler, world);
    }
}
