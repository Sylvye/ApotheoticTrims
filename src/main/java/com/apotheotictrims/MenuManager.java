package com.apotheotictrims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public final class MenuManager implements Listener {
    private static final int[] VALUE_SLOTS = {10, 12, 14, 16, 19, 21, 23, 25};
    private final SettingsManager settings;
    private final AbilityManager abilities;
    private ChatInputManager chatInput;

    public MenuManager(SettingsManager settings, AbilityManager abilities) {
        this.settings = settings;
        this.abilities = abilities;
    }

    public void setChatInput(ChatInputManager chatInput) { this.chatInput = chatInput; }

    public void openCatalog(Player player) {
        CatalogHolder holder = new CatalogHolder();
        Inventory inventory = holder.inventory;
        int slot = 0;
        TrimAbility active = abilities.active(player).orElse(null);
        for (TrimAbility ability : TrimAbility.values()) {
            List<Component> lore = new ArrayList<>();
            lore.add(Component.text(ability.description(), NamedTextColor.GRAY));
            lore.add(Component.empty());
            lore.add(Component.text(settings.enabled(ability) ? "Enabled" : "Disabled",
                    settings.enabled(ability) ? NamedTextColor.GREEN : NamedTextColor.RED));
            if (active == ability) lore.add(Component.text("ACTIVE", NamedTextColor.GOLD));
            lore.add(Component.text("Click for details", NamedTextColor.YELLOW));
            inventory.setItem(slot++, item(ability.icon(), ability.displayName(), lore));
        }
        inventory.setItem(18, item(Material.ARMOR_STAND, "Current set", List.of(Component.text(
                active == null ? "No complete matching trim set" : active.displayName(), NamedTextColor.GRAY))));
        if (player.hasPermission("apotheotictrims.admin")) {
            inventory.setItem(22, item(Material.COMPARATOR, "Feedback settings",
                    List.of(Component.text("Action bar, sounds, and particles", NamedTextColor.GRAY))));
        }
        inventory.setItem(26, item(Material.KNOWLEDGE_BOOK, "How it works", List.of(
                Component.text("Wear the same trim pattern on all four armor pieces.", NamedTextColor.GRAY),
                Component.text("Trim materials may differ.", NamedTextColor.GRAY))));
        player.openInventory(inventory);
    }

    public void openDetail(Player player, TrimAbility ability) {
        DetailHolder holder = new DetailHolder(ability);
        Inventory inventory = holder.inventory;
        List<Component> description = new ArrayList<>();
        description.add(Component.text(ability.description(), NamedTextColor.GRAY));
        description.add(Component.text("Requires a full " + ability.displayName() + " set", NamedTextColor.DARK_GRAY));
        inventory.setItem(4, item(ability.icon(), ability.displayName(), description));
        inventory.setItem(47, item(settings.enabled(ability) ? Material.LIME_DYE : Material.GRAY_DYE,
                settings.enabled(ability) ? "Enabled" : "Disabled", List.of(Component.text(
                        player.hasPermission("apotheotictrims.admin") ? "Click to toggle" : "Admin controlled", NamedTextColor.GRAY))));
        int index = 0;
        for (SettingSpec spec : ability.settings()) {
            int valueSlot = VALUE_SLOTS[index];
            double value = settings.value(ability, spec.key());
            inventory.setItem(valueSlot, item(Material.PAPER, spec.label() + ": " + spec.format(value), List.of(
                    Component.text("Left/right: +/- " + spec.format(spec.step()), NamedTextColor.GRAY),
                    Component.text("Shift-left/right: +/- " + spec.format(spec.coarseStep()), NamedTextColor.GRAY),
                    Component.text("Allowed: " + spec.rangeDescription(), NamedTextColor.DARK_GRAY))));
            inventory.setItem(valueSlot + 1, item(Material.NAME_TAG, "Enter exact value",
                    List.of(Component.text("Click, then type the number in chat", NamedTextColor.GRAY))));
            index++;
        }
        inventory.setItem(45, item(Material.ARROW, "Back", List.of()));
        if (player.hasPermission("apotheotictrims.admin")) {
            inventory.setItem(49, item(Material.BARRIER, "Reset ability", List.of(Component.text("Restore defaults", NamedTextColor.GRAY))));
        }
        player.openInventory(inventory);
    }

    private void openFeedback(Player player) {
        FeedbackHolder holder = new FeedbackHolder();
        holder.inventory.setItem(11, feedbackItem("action-bar", settings.actionBar()));
        holder.inventory.setItem(13, feedbackItem("sounds", settings.sounds()));
        holder.inventory.setItem(15, feedbackItem("particles", settings.particles()));
        holder.inventory.setItem(18, item(Material.ARROW, "Back", List.of()));
        player.openInventory(holder.inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder rawHolder = event.getView().getTopInventory().getHolder(false);
        if (!(rawHolder instanceof PluginMenuHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getRawSlot() < 0
                || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        int slot = event.getRawSlot();
        if (rawHolder instanceof CatalogHolder) {
            if (slot < TrimAbility.values().length) openDetail(player, TrimAbility.values()[slot]);
            else if (slot == 22 && player.hasPermission("apotheotictrims.admin")) openFeedback(player);
            return;
        }
        if (rawHolder instanceof FeedbackHolder) {
            if (slot == 18) openCatalog(player);
            if (!player.hasPermission("apotheotictrims.admin")) return;
            String key = switch (slot) { case 11 -> "action-bar"; case 13 -> "sounds"; case 15 -> "particles"; default -> null; };
            if (key != null) {
                settings.setFeedback(key, !settings.feedback(key));
                openFeedback(player);
            }
            return;
        }
        DetailHolder detail = (DetailHolder) rawHolder;
        TrimAbility ability = detail.ability;
        if (slot == 45) { openCatalog(player); return; }
        if (!player.hasPermission("apotheotictrims.admin")) return;
        if (slot == 47) {
            settings.toggle(ability);
            abilities.refreshAll();
            openDetail(player, ability);
            return;
        }
        if (slot == 49) {
            settings.reset(ability);
            abilities.refreshAll();
            openDetail(player, ability);
            return;
        }
        for (int i = 0; i < ability.settings().size(); i++) {
            SettingSpec spec = ability.settings().get(i);
            int valueSlot = VALUE_SLOTS[i];
            if (slot == valueSlot + 1) {
                chatInput.prompt(player, ability, spec);
                return;
            }
            if (slot == valueSlot) {
                double delta = event.isShiftClick() ? spec.coarseStep() : spec.step();
                if (event.isRightClick()) delta = -delta;
                double value = spec.clamp(settings.value(ability, spec.key()) + delta);
                settings.setValue(ability, spec.key(), value);
                abilities.refreshAll();
                openDetail(player, ability);
                return;
            }
        }
    }

    private ItemStack feedbackItem(String key, boolean enabled) {
        return item(enabled ? Material.LIME_DYE : Material.GRAY_DYE,
                pretty(key) + ": " + (enabled ? "On" : "Off"), List.of(Component.text("Click to toggle", NamedTextColor.GRAY)));
    }

    private static ItemStack item(Material material, String name, List<Component> lore) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.AQUA));
        meta.lore(lore);
        stack.setItemMeta(meta);
        return stack;
    }

    private static String pretty(String key) {
        String value = key.replace('-', ' ');
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private interface PluginMenuHolder extends InventoryHolder {}

    private static final class CatalogHolder implements PluginMenuHolder {
        private final Inventory inventory = Bukkit.createInventory(this, 27, Component.text("Apotheotic Trims"));
        @Override public Inventory getInventory() { return inventory; }
    }

    private static final class DetailHolder implements PluginMenuHolder {
        private final TrimAbility ability;
        private final Inventory inventory;
        private DetailHolder(TrimAbility ability) {
            this.ability = ability;
            this.inventory = Bukkit.createInventory(this, 54, Component.text(ability.displayName() + " settings"));
        }
        @Override public Inventory getInventory() { return inventory; }
    }

    private static final class FeedbackHolder implements PluginMenuHolder {
        private final Inventory inventory = Bukkit.createInventory(this, 27, Component.text("Feedback settings"));
        @Override public Inventory getInventory() { return inventory; }
    }
}
