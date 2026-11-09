package com.apotheotictrims;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class ApotheoticTrimsPlugin extends JavaPlugin {
    private SettingsManager settings;
    private AbilityManager abilities;
    private ChatInputManager chatInput;
    private SilenceEquipmentListener silenceEquipment;

    @Override
    public void onEnable() {
        try {
            settings = new SettingsManager(getDataFolder().toPath(), getLogger());
            settings.load();
        } catch (RuntimeException ex) {
            getLogger().severe("Could not load ApotheoticTrims settings: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        abilities = new AbilityManager(this, settings);
        MenuManager menus = new MenuManager(settings, abilities);
        chatInput = new ChatInputManager(this, settings, abilities, menus);
        menus.setChatInput(chatInput);
        AbilityListener abilityListener = new AbilityListener(this, settings, abilities);

        getServer().getPluginManager().registerEvents(abilityListener, this);
        getServer().getPluginManager().registerEvents(menus, this);
        getServer().getPluginManager().registerEvents(chatInput, this);

        PluginCommand command = getCommand("trims");
        if (command == null) throw new IllegalStateException("The trims command is missing from plugin.yml");
        TrimsCommand handler = new TrimsCommand(settings, abilities, menus);
        command.setExecutor(handler);
        command.setTabCompleter(handler);

        silenceEquipment = new SilenceEquipmentListener(abilities.silence());
        PacketEvents.getAPI().getEventManager().registerListener(silenceEquipment);
        abilities.start();
        getLogger().info("ApotheoticTrims enabled with " + TrimAbility.values().length + " trim abilities.");
    }

    @Override
    public void onDisable() {
        if (chatInput != null) chatInput.clear();
        if (abilities != null) abilities.stop();
        if (silenceEquipment != null) PacketEvents.getAPI().getEventManager().unregisterListener(silenceEquipment);
    }
}
