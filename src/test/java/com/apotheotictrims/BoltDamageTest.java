package com.apotheotictrims;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BoltDamageTest {
    @Test
    void addsBonusToTriggeringHitWithoutDamagingAgain() {
        ApotheoticTrimsPlugin plugin = mock(ApotheoticTrimsPlugin.class);
        SettingsManager settings = mock(SettingsManager.class);
        AbilityManager abilities = mock(AbilityManager.class);
        ComboTracker combos = new ComboTracker();
        when(abilities.combos()).thenReturn(combos);
        when(settings.intValue(TrimAbility.BOLT, "combo-hits")).thenReturn(1);
        when(settings.value(TrimAbility.BOLT, "timeout-seconds")).thenReturn(5.0);
        when(settings.value(TrimAbility.BOLT, "bonus-damage")).thenReturn(10.0, 7.0);
        Player attacker = mock(Player.class);
        when(attacker.getUniqueId()).thenReturn(UUID.randomUUID());
        when(abilities.has(attacker, TrimAbility.BOLT)).thenReturn(true);
        LivingEntity target = mock(LivingEntity.class);
        World world = mock(World.class);
        when(target.getLocation()).thenReturn(new Location(world, 0, 64, 0));
        DamageSource source = mock(DamageSource.class);
        when(source.getCausingEntity()).thenReturn(attacker);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        when(scheduler.runTask(eq(plugin), any(Runnable.class))).thenAnswer(call -> {
            call.<Runnable>getArgument(1).run();
            return mock(BukkitTask.class);
        });
        AbilityListener listener = new AbilityListener(plugin, settings, abilities);

        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            EntityDamageByEntityEvent defaultHit = hit(target, attacker, source);
            listener.onDamage(defaultHit);
            verify(defaultHit).setDamage(24.0);

            EntityDamageByEntityEvent customHit = hit(target, attacker, source);
            listener.onDamage(customHit);
            verify(customHit).setDamage(21.0);
        }
        verify(world, times(2)).strikeLightningEffect(any(Location.class));
        verify(target, never()).damage(anyDouble(), any(DamageSource.class));
    }

    @Test
    void cancelledHitDoesNotAdvanceCombo() {
        AbilityManager abilities = mock(AbilityManager.class);
        AbilityListener listener = new AbilityListener(mock(ApotheoticTrimsPlugin.class),
                mock(SettingsManager.class), abilities);
        EntityDamageByEntityEvent cancelled = mock(EntityDamageByEntityEvent.class);
        when(cancelled.isCancelled()).thenReturn(true);

        listener.onDamage(cancelled);

        verifyNoInteractions(abilities);
        verify(cancelled, never()).setDamage(anyDouble());
    }

    private static EntityDamageByEntityEvent hit(LivingEntity target, Player attacker, DamageSource source) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        when(event.getEntity()).thenReturn(target);
        when(event.getDamager()).thenReturn(attacker);
        when(event.getDamageSource()).thenReturn(source);
        when(event.getDamage()).thenReturn(14.0);
        return event;
    }
}
