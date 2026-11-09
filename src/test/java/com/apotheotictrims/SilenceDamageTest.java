package com.apotheotictrims;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SilenceDamageTest {
    private final AbilityManager abilities = mock(AbilityManager.class);
    private final SilenceManager silence = mock(SilenceManager.class);
    private final ApotheoticTrimsPlugin plugin = mock(ApotheoticTrimsPlugin.class);
    private final AbilityListener listener = new AbilityListener(plugin,
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

    @Test void resetsOnZeroAndFractionalDamageFromAnyCreditedHit() {
        when(abilities.silence()).thenReturn(silence);
        WindCharge charge = mock(WindCharge.class);
        when(charge.getShooter()).thenReturn(attacker);
        Arrow arrow = mock(Arrow.class);
        when(arrow.getShooter()).thenReturn(attacker);
        for (Entity damager : new Entity[] { attacker, charge, arrow }) {
            for (double amount : new double[] { 0.0, 0.001 }) {
                EntityDamageByEntityEvent damage = hit(damager, null);
                when(damage.getFinalDamage()).thenReturn(amount);
                listener.onSilenceDamage(damage);
            }
        }
        // Attribution also applies to damage that is not an entity-hit event.
        EntityDamageEvent damage = mock(EntityDamageEvent.class);
        DamageSource source = mock(DamageSource.class);
        when(source.getCausingEntity()).thenReturn(attacker);
        when(damage.getDamageSource()).thenReturn(source);
        when(damage.getEntity()).thenReturn(victim);
        listener.onSilenceDamage(damage);
        verify(silence, times(7)).clear(victim);
    }

    @Test void ignoresCancelledSelfHitsMobsAndEnvironment() {
        EntityDamageByEntityEvent cancelled = hit(attacker, attacker);
        when(cancelled.isCancelled()).thenReturn(true);
        listener.onSilenceDamage(cancelled);
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

    @Test void meleeWithoutDamageEventStillResetsSilence() {
        PrePlayerAttackEntityEvent attack = new PrePlayerAttackEntityEvent(attacker, victim, true);
        Runnable reset = runDeferredContact(() -> listener.onSilenceMeleeContact(attack), () -> {});
        verify(reset).run();
    }

    @Test void cancelledProjectileDamageSuppressesContactFallback() {
        WindCharge charge = mock(WindCharge.class);
        when(charge.getShooter()).thenReturn(attacker);
        when(charge.getUniqueId()).thenReturn(UUID.randomUUID());
        EntityDamageByEntityEvent damage = hit(charge, attacker);
        when(damage.isCancelled()).thenReturn(true);
        Runnable reset = runDeferredContact(
                () -> listener.onSilenceProjectileContact(new ProjectileHitEvent(charge, victim)),
                () -> listener.onSilenceDamage(damage));
        verifyNoInteractions(reset);
        verify(silence, never()).clear(victim);
    }

    @Test void projectileWithoutDamageEventStillResetsSilence() {
        WindCharge charge = mock(WindCharge.class);
        when(charge.getShooter()).thenReturn(attacker);
        when(charge.getUniqueId()).thenReturn(UUID.randomUUID());
        Runnable reset = runDeferredContact(
                () -> listener.onSilenceProjectileContact(new ProjectileHitEvent(charge, victim)), () -> {});
        verify(reset).run();
    }

    private Runnable runDeferredContact(Runnable contact, Runnable damage) {
        Runnable reset = mock(Runnable.class);
        when(abilities.silence()).thenReturn(silence);
        when(silence.resetCurrentCharge(victim)).thenReturn(reset);
        BukkitScheduler scheduler = mock(BukkitScheduler.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            contact.run();
            damage.run();
            var task = ArgumentCaptor.forClass(Runnable.class);
            verify(scheduler).runTask(eq(plugin), task.capture());
            verifyNoInteractions(reset);
            task.getValue().run();
        }
        return reset;
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
