package com.apotheotictrims;

import org.bukkit.damage.DamageSource;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SilenceDamageTest {
    private final AbilityManager abilities = mock(AbilityManager.class);
    private final SilenceManager silence = mock(SilenceManager.class);
    private final AbilityListener listener = new AbilityListener(mock(ApotheoticTrimsPlugin.class),
            mock(SettingsManager.class), abilities);
    private final Player victim = player(), attacker = player();

    @Test void resetsOnMeleePlayerProjectilesAndOwnedPets() {
        when(abilities.silence()).thenReturn(silence);
        EntityDamageByEntityEvent melee = hit(attacker, attacker);
        listener.onSilenceDamage(melee);
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(attacker);
        listener.onSilenceDamage(hit(arrow, null));
        Wolf wolf = mock(Wolf.class);
        when(wolf.getOwner()).thenReturn(attacker);
        listener.onSilenceDamage(hit(wolf, wolf));
        listener.onSilenceDamage(hit(mock(Arrow.class), attacker));
        verify(silence, times(4)).clear(victim);
    }

    @Test void ignoresCancelledZeroDamageSelfHitsMobsAndEnvironment() {
        EntityDamageByEntityEvent cancelled = hit(attacker, attacker);
        when(cancelled.isCancelled()).thenReturn(true);
        listener.onSilenceDamage(cancelled);
        EntityDamageByEntityEvent blocked = hit(attacker, attacker);
        when(blocked.getFinalDamage()).thenReturn(0.0);
        listener.onSilenceDamage(blocked);
        listener.onSilenceDamage(hit(victim, victim));
        listener.onSilenceDamage(hit(mock(Zombie.class), null));
        EntityDamageEvent fall = mock(EntityDamageEvent.class);
        when(fall.getEntity()).thenReturn(victim);
        when(fall.getFinalDamage()).thenReturn(4.0);
        when(fall.getDamageSource()).thenReturn(mock(DamageSource.class));
        listener.onSilenceDamage(fall);
        verify(abilities, never()).silence();
    }

    @Test void attackingDoesNotResetAttackerAndInstantDamageIsNoLongerCancelled() {
        when(abilities.silence()).thenReturn(silence);
        when(abilities.has(victim, TrimAbility.SILENCE)).thenReturn(true);
        ThrownPotion potion = mock(ThrownPotion.class);
        when(potion.getShooter()).thenReturn(attacker);
        EntityDamageByEntityEvent damage = hit(potion, attacker);
        listener.onDamage(damage);
        listener.onSilenceDamage(damage);
        verify(damage, never()).setCancelled(anyBoolean());
        verify(silence).clear(victim);
        verify(silence, never()).clear(attacker);
    }

    private EntityDamageByEntityEvent hit(Entity damager, Entity causing) {
        EntityDamageByEntityEvent event = mock(EntityDamageByEntityEvent.class);
        DamageSource source = mock(DamageSource.class);
        when(source.getCausingEntity()).thenReturn(causing);
        when(event.getDamageSource()).thenReturn(source);
        when(event.getEntity()).thenReturn(victim);
        when(event.getDamager()).thenReturn(damager);
        when(event.getFinalDamage()).thenReturn(4.0);
        return event;
    }

    private static Player player() {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        return player;
    }
}
