package com.apotheotictrims;

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.UUID;

import static org.mockito.Mockito.*;

class WildFallDamageTest {
    @Test
    void wildCancelsEveryFallOnlyWhileSetIsActive() {
        ApotheoticTrimsPlugin plugin = mock(ApotheoticTrimsPlugin.class);
        SettingsManager settings = mock(SettingsManager.class);
        AbilityManager abilities = mock(AbilityManager.class);
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(abilities.spireFallProtection()).thenReturn(new HashSet<>());
        when(abilities.has(player, TrimAbility.WILD)).thenReturn(true, true, false);
        AbilityListener listener = new AbilityListener(plugin, settings, abilities);

        EntityDamageEvent first = fallEvent(player);
        listener.onEnvironmentalDamage(first);
        verify(first).setCancelled(true);

        EntityDamageEvent second = fallEvent(player);
        listener.onEnvironmentalDamage(second);
        verify(second).setCancelled(true);

        EntityDamageEvent afterRemoval = fallEvent(player);
        listener.onEnvironmentalDamage(afterRemoval);
        verify(afterRemoval, never()).setCancelled(true);
    }

    private static EntityDamageEvent fallEvent(Player player) {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.FALL);
        return event;
    }
}
