package com.apotheotictrims;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class TrimsCommand implements CommandExecutor, TabCompleter {
    private final SettingsManager settings;
    private final AbilityManager abilities;
    private final MenuManager menus;

    public TrimsCommand(SettingsManager settings, AbilityManager abilities, MenuManager menus) {
        this.settings = settings;
        this.abilities = abilities;
        this.menus = menus;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) return fail(sender, "Only players can open the trim GUI.");
            if (!sender.hasPermission("apotheotictrims.info")) return fail(sender, "You do not have permission.");
            menus.openCatalog(player);
            return true;
        }
        return switch (args[0].toLowerCase(Locale.ROOT)) {
            case "help" -> help(sender);
            case "info" -> info(sender, args);
            case "status" -> status(sender, args);
            case "settings" -> settings(sender);
            case "set" -> set(sender, args);
            case "toggle" -> toggle(sender, args);
            case "reset" -> reset(sender, args);
            default -> fail(sender, "Unknown subcommand. Use /trims help.");
        };
    }

    private boolean help(CommandSender sender) {
        sender.sendMessage(Component.text("ApotheoticTrims commands", NamedTextColor.AQUA));
        sender.sendMessage(Component.text("/trims - open the trim catalog", NamedTextColor.GRAY));
        sender.sendMessage(Component.text("/trims info [trim] | /trims status [player]", NamedTextColor.GRAY));
        if (sender.hasPermission("apotheotictrims.admin")) {
            sender.sendMessage(Component.text("/trims settings", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/trims set <trim|feedback> <setting> <value>", NamedTextColor.GRAY));
            sender.sendMessage(Component.text("/trims toggle <trim> | /trims reset <trim|all>", NamedTextColor.GRAY));
        }
        return true;
    }

    private boolean info(CommandSender sender, String[] args) {
        if (!sender.hasPermission("apotheotictrims.info")) return fail(sender, "You do not have permission.");
        if (args.length < 2) {
            if (sender instanceof Player player) { menus.openCatalog(player); return true; }
            return fail(sender, "Usage: /trims info <trim>");
        }
        TrimAbility ability = ability(args[1]);
        if (ability == null) return fail(sender, "Unknown trim: " + args[1]);
        if (sender instanceof Player player) menus.openDetail(player, ability);
        else sendInfo(sender, ability);
        return true;
    }

    private void sendInfo(CommandSender sender, TrimAbility ability) {
        sender.sendMessage(Component.text(ability.displayName() + ": " + ability.description(), NamedTextColor.AQUA));
        sender.sendMessage(Component.text("Enabled: " + settings.enabled(ability), NamedTextColor.GRAY));
        ability.settings().forEach(spec -> sender.sendMessage(Component.text(
                spec.key() + " = " + spec.format(settings.value(ability, spec.key())), NamedTextColor.GRAY)));
    }

    private boolean status(CommandSender sender, String[] args) {
        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission("apotheotictrims.admin")) return fail(sender, "You do not have permission to inspect others.");
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) return fail(sender, "That player is not online.");
        } else if (sender instanceof Player player) target = player;
        else return fail(sender, "Usage: /trims status <player>");
        String state = abilities.active(target).map(TrimAbility::displayName).orElse("No active full trim set");
        sender.sendMessage(Component.text(target.getName() + ": " + state, NamedTextColor.AQUA));
        return true;
    }

    private boolean settings(CommandSender sender) {
        if (!admin(sender)) return false;
        if (!(sender instanceof Player player)) return fail(sender, "Only players can open the settings GUI.");
        menus.openCatalog(player);
        return true;
    }

    private boolean set(CommandSender sender, String[] args) {
        if (!admin(sender)) return false;
        if (args.length != 4) return fail(sender, "Usage: /trims set <trim|feedback> <setting> <value>");
        try {
            if (args[1].equalsIgnoreCase("feedback")) {
                Boolean value = booleanValue(args[3]);
                if (value == null) return fail(sender, "Feedback values must be on/off or true/false.");
                settings.setFeedback(args[2], value);
            } else {
                TrimAbility ability = ability(args[1]);
                if (ability == null) return fail(sender, "Unknown trim: " + args[1]);
                settings.setValue(ability, args[2], Double.parseDouble(args[3]));
                abilities.refreshAll();
            }
            sender.sendMessage(Component.text("Setting updated.", NamedTextColor.GREEN));
            return true;
        } catch (NumberFormatException ex) {
            return fail(sender, "Enter a valid number.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return fail(sender, ex.getMessage());
        }
    }

    private boolean toggle(CommandSender sender, String[] args) {
        if (!admin(sender)) return false;
        if (args.length != 2) return fail(sender, "Usage: /trims toggle <trim>");
        TrimAbility ability = ability(args[1]);
        if (ability == null) return fail(sender, "Unknown trim: " + args[1]);
        settings.toggle(ability);
        abilities.refreshAll();
        sender.sendMessage(Component.text(ability.displayName() + " is now "
                + (settings.enabled(ability) ? "enabled" : "disabled") + ".", NamedTextColor.GREEN));
        return true;
    }

    private boolean reset(CommandSender sender, String[] args) {
        if (!admin(sender)) return false;
        if (args.length != 2) return fail(sender, "Usage: /trims reset <trim|all>");
        if (args[1].equalsIgnoreCase("all")) settings.resetAll();
        else {
            TrimAbility ability = ability(args[1]);
            if (ability == null) return fail(sender, "Unknown trim: " + args[1]);
            settings.reset(ability);
        }
        abilities.refreshAll();
        sender.sendMessage(Component.text("Defaults restored.", NamedTextColor.GREEN));
        return true;
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("apotheotictrims.admin")) return true;
        fail(sender, "You do not have permission.");
        return false;
    }

    private static TrimAbility ability(String value) { return TrimAbility.fromKey(value).orElse(null); }
    private static Boolean booleanValue(String value) {
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "on", "yes" -> true;
            case "false", "off", "no" -> false;
            default -> null;
        };
    }
    private static boolean fail(CommandSender sender, String message) {
        sender.sendMessage(Component.text(message, NamedTextColor.RED));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) return filter(args[0], sender.hasPermission("apotheotictrims.admin")
                ? List.of("help", "info", "status", "settings", "set", "toggle", "reset")
                : List.of("help", "info", "status"));
        String sub = args[0].toLowerCase(Locale.ROOT);
        if (args.length == 2 && sub.equals("status") && sender.hasPermission("apotheotictrims.admin"))
            return filter(args[1], Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
        if (args.length == 2 && List.of("info", "toggle").contains(sub)) return filter(args[1], abilityKeys());
        if (args.length == 2 && sub.equals("reset")) {
            List<String> choices = new ArrayList<>(abilityKeys()); choices.add("all"); return filter(args[1], choices);
        }
        if (args.length == 2 && sub.equals("set")) {
            List<String> choices = new ArrayList<>(abilityKeys()); choices.add("feedback"); return filter(args[1], choices);
        }
        if (args.length == 3 && sub.equals("set")) {
            if (args[1].equalsIgnoreCase("feedback")) return filter(args[2], List.of("action-bar", "sounds", "particles"));
            TrimAbility ability = ability(args[1]);
            if (ability != null) return filter(args[2], ability.settings().stream().map(SettingSpec::key).toList());
        }
        if (args.length == 4 && sub.equals("set") && args[1].equalsIgnoreCase("feedback"))
            return filter(args[3], List.of("on", "off"));
        return List.of();
    }

    private static List<String> abilityKeys() { return Arrays.stream(TrimAbility.values()).map(TrimAbility::key).toList(); }
    private static List<String> filter(String prefix, List<String> choices) {
        String lower = prefix.toLowerCase(Locale.ROOT);
        return choices.stream().filter(choice -> choice.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }
}
