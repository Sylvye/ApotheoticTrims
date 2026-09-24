package com.apotheotictrims;

import org.bukkit.entity.Player;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;

import java.util.List;
import java.util.Optional;

public final class TrimDetector {
    public Optional<TrimAbility> detect(Player player) {
        PlayerInventory inventory = player.getInventory();
        return detect(inventory.getHelmet(), inventory.getChestplate(), inventory.getLeggings(), inventory.getBoots());
    }

    public Optional<TrimAbility> detect(ItemStack helmet, ItemStack chestplate, ItemStack leggings, ItemStack boots) {
        String first = patternKey(helmet);
        if (first == null || !first.equals(patternKey(chestplate)) || !first.equals(patternKey(leggings))
                || !first.equals(patternKey(boots))) {
            return Optional.empty();
        }
        return TrimAbility.fromKey(first);
    }

    static Optional<String> commonPattern(List<String> patterns) {
        if (patterns.size() != 4 || patterns.getFirst() == null) return Optional.empty();
        return patterns.stream().allMatch(patterns.getFirst()::equals) ? Optional.of(patterns.getFirst()) : Optional.empty();
    }

    private String patternKey(ItemStack item) {
        if (item == null || !(item.getItemMeta() instanceof ArmorMeta armorMeta) || !armorMeta.hasTrim()) return null;
        return Registry.TRIM_PATTERN.getKeyOrThrow(armorMeta.getTrim().getPattern()).getKey();
    }
}
