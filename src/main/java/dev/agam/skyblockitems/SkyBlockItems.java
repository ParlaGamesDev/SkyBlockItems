package dev.agam.skyblockitems;

import dev.agam.skyblockitems.abilities.AbilityManager;
import dev.agam.skyblockitems.integration.MMOItemsHook;
import org.bukkit.plugin.java.JavaPlugin;

public class SkyBlockItems extends JavaPlugin {

    private static SkyBlockItems instance;
    private AbilityManager abilityManager;
    private MMOItemsHook mmoItemsHook;
    private org.bukkit.configuration.file.FileConfiguration abilitiesConfig;
    private org.bukkit.configuration.file.FileConfiguration messagesConfig;

    public static SkyBlockItems getInstance() {
        return instance;
    }

    public AbilityManager getAbilityManager() {
        return abilityManager;
    }

    @Override
    public void onLoad() {
        // Register WorldGuard flags (must be done in onLoad)
        try {
            if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                dev.agam.skyblockitems.integration.WorldGuardHook.registerFlags();
            }
        } catch (NoClassDefFoundError ignored) {
        }
    }

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadCustomConfigs();

        // Initialize Ability Manager
        this.abilityManager = new AbilityManager();
        this.abilityManager.registerAbilities();

        // Initialize MMOItems Hook
        this.mmoItemsHook = new MMOItemsHook();
        this.mmoItemsHook.registerStats();

        // Register Listeners
        getServer().getPluginManager().registerEvents(new dev.agam.skyblockitems.abilities.AbilityListener(), this);
        getServer().getPluginManager().registerEvents(new dev.agam.skyblockitems.integration.MMOItemsAbilityListener(),
                this);
        getServer().getPluginManager().registerEvents(new dev.agam.skyblockitems.listeners.InfiniteReservoirListener(),
                this);

        // Start Passive Tasks
        new dev.agam.skyblockitems.tasks.PassiveAbilityTask().runTaskTimer(this, 20L, 20L);

        // Register Commands
        dev.agam.skyblockitems.commands.SkyBlockItemsCommand sbiCmd = new dev.agam.skyblockitems.commands.SkyBlockItemsCommand();
        getCommand("skyblockitems").setExecutor(sbiCmd);
        getCommand("skyblockitems").setTabCompleter(sbiCmd);

        getLogger().info("SkyBlockItems has been enabled!");
    }

    @Override
    public void onDisable() {
        // Cancel all active tasks to prevent task execution after unload
        getServer().getScheduler().cancelTasks(this);
        getLogger().info("SkyBlockItems has been disabled!");
    }

    private void loadCustomConfigs() {
        // Load abilities.yml
        java.io.File abilitiesFile = new java.io.File(getDataFolder(), "abilities.yml");
        if (!abilitiesFile.exists()) {
            saveResource("abilities.yml", false);
        }
        abilitiesConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(abilitiesFile);

        // Load messages.yml
        java.io.File messagesFile = new java.io.File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(messagesFile);
    }

    public org.bukkit.configuration.file.FileConfiguration getAbilitiesConfig() {
        return abilitiesConfig;
    }

    public org.bukkit.configuration.file.FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    /**
     * Reloads all configuration files (config.yml, abilities.yml, messages.yml)
     */
    public void reloadAllConfigs() {
        reloadConfig(); // Reload config.yml
        loadCustomConfigs(); // Reload abilities.yml and messages.yml
        getLogger().info("All configurations reloaded!");
    }
}
