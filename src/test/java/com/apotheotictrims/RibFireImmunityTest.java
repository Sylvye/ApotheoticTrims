package com.apotheotictrims;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.potion.PotionEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RibFireImmunityTest {
    @Test
    void regenerationRunsLongEnoughToHealBeforeRefresh() {
        assertTrue(AbilityManager.regenerationNeedsRefresh(null, 2));
        PotionEffect effect = mock(PotionEffect.class);
        when(effect.getAmplifier()).thenReturn(1);
        when(effect.getDuration()).thenReturn(25, 11, 10);
        assertFalse(AbilityManager.regenerationNeedsRefresh(effect, 2));
        assertFalse(AbilityManager.regenerationNeedsRefresh(effect, 2));
        assertTrue(AbilityManager.regenerationNeedsRefresh(effect, 2));
    }

    @Test
    void cancelsFireDamageOnlyForRibWithoutExtinguishing() {
        AbilityManager abilities = mock(AbilityManager.class);
        Player player = mock(Player.class);
        when(player.getFireTicks()).thenReturn(80);
        AbilityListener listener = new AbilityListener(mock(ApotheoticTrimsPlugin.class),
                mock(SettingsManager.class), abilities);

        for (EntityDamageEvent.DamageCause cause : new EntityDamageEvent.DamageCause[] {
                EntityDamageEvent.DamageCause.FIRE, EntityDamageEvent.DamageCause.FIRE_TICK,
                EntityDamageEvent.DamageCause.LAVA, EntityDamageEvent.DamageCause.HOT_FLOOR,
                EntityDamageEvent.DamageCause.CAMPFIRE
        }) {
            EntityDamageEvent event = damageEvent(player, cause);
            when(abilities.has(player, TrimAbility.RIB)).thenReturn(true);
            listener.onEnvironmentalDamage(event);
            verify(event).setCancelled(true);

            EntityDamageEvent withoutRib = damageEvent(player, cause);
            when(abilities.has(player, TrimAbility.RIB)).thenReturn(false);
            listener.onEnvironmentalDamage(withoutRib);
            verify(withoutRib, never()).setCancelled(true);
        }
        verify(player, never()).setFireTicks(anyInt());
        assertEquals(80, player.getFireTicks());
    }

    @Test
    void contactDamageIsCancelledOnlyForHotBlocks() {
        AbilityManager abilities = mock(AbilityManager.class);
        Player player = mock(Player.class);
        when(abilities.has(player, TrimAbility.RIB)).thenReturn(true);
        AbilityListener listener = new AbilityListener(mock(ApotheoticTrimsPlugin.class),
                mock(SettingsManager.class), abilities);

        for (Material material : new Material[] {Material.MAGMA_BLOCK, Material.CAMPFIRE, Material.SOUL_CAMPFIRE}) {
            EntityDamageByBlockEvent event = contactEvent(player, material);
            listener.onEnvironmentalDamage(event);
            verify(event).setCancelled(true);
        }

        EntityDamageByBlockEvent cactus = contactEvent(player, Material.CACTUS);
        listener.onEnvironmentalDamage(cactus);
        verify(cactus, never()).setCancelled(true);

        when(abilities.has(player, TrimAbility.RIB)).thenReturn(false);
        EntityDamageByBlockEvent withoutRib = contactEvent(player, Material.MAGMA_BLOCK);
        listener.onEnvironmentalDamage(withoutRib);
        verify(withoutRib, never()).setCancelled(true);
    }

    private static EntityDamageByBlockEvent contactEvent(Player player, Material material) {
        EntityDamageByBlockEvent event = mock(EntityDamageByBlockEvent.class);
        Block block = mock(Block.class);
        when(block.getType()).thenReturn(material);
        when(event.getDamager()).thenReturn(block);
        when(event.getEntity()).thenReturn(player);
        when(event.getCause()).thenReturn(EntityDamageEvent.DamageCause.CONTACT);
        return event;
    }

    private static EntityDamageEvent damageEvent(Player player, EntityDamageEvent.DamageCause cause) {
        EntityDamageEvent event = mock(EntityDamageEvent.class);
        when(event.getEntity()).thenReturn(player);
        when(event.getCause()).thenReturn(cause);
        return event;
    }
}
