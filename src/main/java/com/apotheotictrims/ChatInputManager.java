package com.apotheotictrims;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ChatInputManager implements Listener {
    private static final long TIMEOUT_MILLIS = 60_000;
    private final ApotheoticTrimsPlugin plugin;
    private final SettingsManager settings;
    private final AbilityManager abilities;
    private final MenuManager menus;
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

    public ChatInputManager(ApotheoticTrimsPlugin plugin, SettingsManager settings,
                            AbilityManager abilities, MenuManager menus) {
        this.plugin = plugin;
        this.settings = settings;
        this.abilities = abilities;
        this.menus = menus;
    }

    public void prompt(Player player, TrimAbility ability, SettingSpec spec) {
        pending.put(player.getUniqueId(), new PendingInput(ability, spec, System.currentTimeMillis() + TIMEOUT_MILLIS));
        player.closeInventory();
        player.sendMessage(Component.text("Enter " + spec.label() + " (" + spec.rangeDescription()
                + "), or type cancel. This expires in 60 seconds.", NamedTextColor.YELLOW));
    }

    public void clear() { pending.clear(); }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PendingInput input = pending.remove(player.getUniqueId());
        if (input == null) return;
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();
        Bukkit.getScheduler().runTask(plugin, () -> handle(player, input, text));
    }

    private void handle(Player player, PendingInput input, String text) {
        if (!player.isOnline()) return;
        if (System.currentTimeMillis() > input.expiresAt()) {
            player.sendMessage(Component.text("That settings prompt expired.", NamedTextColor.RED));
            menus.openDetail(player, input.ability());
            return;
        }
        if (text.equalsIgnoreCase("cancel")) {
            player.sendMessage(Component.text("Edit cancelled.", NamedTextColor.GRAY));
            menus.openDetail(player, input.ability());
            return;
        }
        try {
            double value = Double.parseDouble(text);
            settings.setValue(input.ability(), input.spec().key(), value);
            abilities.refreshAll();
            player.sendMessage(Component.text(input.spec().label() + " set to " + input.spec().format(value) + ".",
                    NamedTextColor.GREEN));
        } catch (NumberFormatException ex) {
            player.sendMessage(Component.text("Enter a valid number.", NamedTextColor.RED));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            player.sendMessage(Component.text(ex.getMessage(), NamedTextColor.RED));
        }
        menus.openDetail(player, input.ability());
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) { pending.remove(event.getPlayer().getUniqueId()); }

    private record PendingInput(TrimAbility ability, SettingSpec spec, long expiresAt) {}
}
