package com.apotheotictrims;

import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SilenceVisibilityTest {
    @BeforeAll static void initializeApiRegistry() {
        // Paper's API needs a registry provider even when potion types are mocked.
        try (MockedStatic<RegistryAccess> registries = mockStatic(RegistryAccess.class)) {
            RegistryAccess access = mock(RegistryAccess.class, call -> mock(Registry.class, entry -> {
                if (entry.getMethod().getName().equals("getOrThrow")) return mock(PotionEffectType.class);
                return RETURNS_DEFAULTS.answer(entry);
            }));
            registries.when(RegistryAccess::registryAccess).thenReturn(access);
            mock(PotionEffectType.class);
        }
    }

    private final PotionEffectType type = mock(PotionEffectType.class);
    private final SilenceManager.PotionVisibility visibility = new SilenceManager.PotionVisibility(type);
    private final Player player = mock(Player.class);
    private final AtomicReference<PotionEffect> current = new AtomicReference<>();

    @BeforeEach void setup() {
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getPotionEffect(type)).thenAnswer(call -> current.get());
        doAnswer(call -> { current.set(null); return null; }).when(player).removePotionEffect(type);
        when(player.addPotionEffect(any(PotionEffect.class))).thenAnswer(call -> {
            current.set(call.getArgument(0));
            return true;
        });
    }

    @Test void showsInvisibilityParticlesAndRestoresPreviousEffectWithElapsedTime() {
        current.set(new PotionEffect(type, 200, 1, true, true, true));
        visibility.start(player, 100);
        visibility.refresh(player, 100);
        assertTrue(current.get().hasParticles());
        assertFalse(current.get().hasIcon());
        assertEquals(3, current.get().getDuration());
        visibility.stop(player, 150);
        assertEquals(150, current.get().getDuration());
        assertEquals(1, current.get().getAmplifier());
        assertTrue(current.get().hasParticles());
        assertTrue(current.get().isAmbient());
    }

    @Test void restoresInfiniteEffectButDoesNotResurrectExpiredEffects() {
        current.set(new PotionEffect(type, -1, 0));
        visibility.start(player, 0);
        visibility.refresh(player, 0);
        visibility.stop(player, 500);
        assertTrue(current.get().isInfinite());
        current.set(new PotionEffect(type, 20, 0));
        visibility.start(player, 500);
        visibility.refresh(player, 500);
        visibility.stop(player, 550);
        assertNull(current.get());
    }

    @Test void newExternalEffectIsPreservedAndUnrelatedNegativeEffectsAreUntouched() {
        visibility.start(player, 0);
        visibility.refresh(player, 0);
        PotionEffect external = new PotionEffect(type, 600, 2);
        visibility.potionChanged(player, change(external), 5);
        current.set(external);
        visibility.refresh(player, 6);
        visibility.stop(player, 15);
        assertEquals(590, current.get().getDuration());
        assertEquals(2, current.get().getAmplifier());

        visibility.start(player, 20);
        visibility.refresh(player, 20);
        EntityPotionEffectEvent poison = mock(EntityPotionEffectEvent.class);
        visibility.potionChanged(player, poison, 21);
        verify(poison, never()).setCancelled(anyBoolean());
        visibility.stop(player, 30);
        assertEquals(580, current.get().getDuration());
    }

    @Test void milkClearsSavedEffectAndOtherPluginsCurrentEffectIsNotRemovedOnCleanup() {
        current.set(new PotionEffect(type, 100, 0));
        visibility.start(player, 0);
        visibility.refresh(player, 0);
        EntityPotionEffectEvent milk = mock(EntityPotionEffectEvent.class);
        when(milk.getModifiedType()).thenReturn(type);
        when(milk.getAction()).thenReturn(EntityPotionEffectEvent.Action.REMOVED);
        visibility.potionChanged(player, milk, 10);
        visibility.stop(player, 11);
        assertNull(current.get());

        visibility.start(player, 12);
        visibility.refresh(player, 12);
        PotionEffect external = new PotionEffect(type, 200, 2);
        current.set(external);
        visibility.stop(player, 13);
        assertSame(external, current.get());
    }

    @Test void masksAllSixSlotsAndRestoresActualEquipmentWithoutEditingInventory() {
        Player viewer = mock(Player.class);
        PlayerInventory inventory = mock(PlayerInventory.class);
        when(player.getInventory()).thenReturn(inventory);
        ItemStack air = mock(ItemStack.class), item = mock(ItemStack.class);
        when(inventory.getItem(any(EquipmentSlot.class))).thenReturn(item);
        try (MockedStatic<ItemStack> items = mockStatic(ItemStack.class)) {
            items.when(ItemStack::empty).thenReturn(air);
            visibility.equipment(viewer, player, true);
            visibility.equipment(viewer, player, false);
        }
        @SuppressWarnings("unchecked") ArgumentCaptor<Map<EquipmentSlot, ItemStack>> captor = ArgumentCaptor.forClass(Map.class);
        verify(viewer, times(2)).sendEquipmentChange(eq(player), captor.capture());
        Map<EquipmentSlot, ItemStack> hidden = captor.getAllValues().get(0);
        assertEquals(6, hidden.size());
        for (EquipmentSlot slot : new EquipmentSlot[] {EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            assertSame(air, hidden.get(slot));
            assertSame(item, captor.getAllValues().get(1).get(slot));
        }
        verify(inventory, never()).setItem(any(EquipmentSlot.class), any());
    }

    private EntityPotionEffectEvent change(PotionEffect effect) {
        EntityPotionEffectEvent event = mock(EntityPotionEffectEvent.class);
        when(event.getModifiedType()).thenReturn(type);
        when(event.getAction()).thenReturn(EntityPotionEffectEvent.Action.CHANGED);
        when(event.isOverride()).thenReturn(true);
        when(event.getNewEffect()).thenReturn(effect);
        return event;
    }
}
