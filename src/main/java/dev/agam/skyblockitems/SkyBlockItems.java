package dev.agam.skyblockitems;

import dev.agam.skyblockitems.abilities.AbilityManager;
import dev.agam.skyblockitems.commands.SkyBlockItemsCommand;
import dev.agam.skyblockitems.enchantsystem.hooks.AuraSkillsHook;
import dev.agam.skyblockitems.enchantsystem.hooks.MMOItemsHook;
import dev.agam.skyblockitems.enchantsystem.managers.ChatInputManager;
import dev.agam.skyblockitems.enchantsystem.config.ConfigManager;
import dev.agam.skyblockitems.enchantsystem.managers.CustomEnchantManager;
import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class SkyBlockItems extends JavaPlugin {

    private static SkyBlockItems instance;
    private AbilityManager abilityManager;
    private dev.agam.skyblockitems.integration.MMOItemsHook mmoItemsStatHook;
    private org.bukkit.configuration.file.FileConfiguration abilitiesConfig;
    private org.bukkit.configuration.file.FileConfiguration messagesConfig;

    // Enchantment System Managers
    private ConfigManager enchantConfigManager;
    private EnchantManager enchantManager;
    private CustomEnchantManager customEnchantManager;
    private ChatInputManager chatInputManager;

    // Enchantment System Hooks
    private AuraSkillsHook auraSkillsHook;
    private MMOItemsHook mmoEnchantHook;
    private boolean auraSkillsEnabled = false;
    private boolean mmoItemsEnabled = false;

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
        getLogger().info("Starting SkyBlockItems initialization...");

        try {
            // Initialize Enchantment System Managers
            getLogger().info("Initializing managers...");
            this.enchantConfigManager = new ConfigManager(this);
            this.chatInputManager = new ChatInputManager();
            this.enchantManager = new EnchantManager(this);
            this.customEnchantManager = new CustomEnchantManager(this);
            getLogger().info("Managers initialized.");

            getLogger().info("Loading configs...");
            loadCustomConfigs();
            getLogger().info("Configs loaded.");

            // Initialize Ability Manager
            getLogger().info("Registering abilities...");
            this.abilityManager = new AbilityManager();
            this.abilityManager.registerAbilities();
            getLogger().info("Abilities registered.");

            // Initialize Integration Hooks
            // Initialize Integration Hooks
            if (Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
                try {
                    getLogger().info("Hooking into MMOItems stats...");
                    this.mmoItemsStatHook = new dev.agam.skyblockitems.integration.MMOItemsHook();
                    this.mmoItemsStatHook.registerStats();
                    getLogger().info("MMOItems stats hooked.");
                } catch (Throwable e) {
                    getLogger().severe("Failed to hook into MMOItems stats! Is the version compatible?");
                    e.printStackTrace();
                }
            } else {
                getLogger().warning("MMOItems not found or not enabled! Stat bonuses will not work.");
            }

            // Enchant System Hooks
            if (Bukkit.getPluginManager().getPlugin("AuraSkills") != null) {
                getLogger().info("Hooking into AuraSkills...");
                this.auraSkillsHook = new AuraSkillsHook();
                this.auraSkillsEnabled = true;
                getLogger().info("Hooked into AuraSkills!");
            }

            if (Bukkit.getPluginManager().getPlugin("MMOItems") != null) {
                getLogger().info("Hooking into MMOItems (Enchant System)...");
                this.mmoEnchantHook = new MMOItemsHook(this);
                this.mmoItemsEnabled = true;
                getLogger().info("Hooked into MMOItems (Enchant System)!");
            }

            // Register Listeners
            getLogger().info("Registering listeners...");
            getServer().getPluginManager().registerEvents(new dev.agam.skyblockitems.abilities.AbilityListener(), this);
            getServer().getPluginManager().registerEvents(
                    new dev.agam.skyblockitems.integration.MMOItemsAbilityListener(),
                    this);
            getServer().getPluginManager().registerEvents(
                    new dev.agam.skyblockitems.listeners.InfiniteReservoirListener(),
                    this);

            // Enchantment System Listeners
            getServer().getPluginManager().registerEvents(
                    new dev.agam.skyblockitems.enchantsystem.listeners.GuiListener(),
                    this);
            getServer().getPluginManager().registerEvents(
                    new dev.agam.skyblockitems.enchantsystem.listeners.CustomEnchantListener(this), this);
            if (auraSkillsEnabled) {
                getServer().getPluginManager().registerEvents(
                        new dev.agam.skyblockitems.enchantsystem.listeners.AuraSkillsListener(this), this);
            }
            getLogger().info("Listeners registered.");

            // Start Passive Tasks
            getLogger().info("Starting passive tasks...");
            new dev.agam.skyblockitems.tasks.PassiveAbilityTask().runTaskTimer(this, 20L, 20L);
            getLogger().info("Passive tasks started.");

            // Register Commands
            getLogger().info("Registering commands...");
            SkyBlockItemsCommand sbiCommand = new SkyBlockItemsCommand(this);
            getCommand("skyblockitems").setExecutor(sbiCommand);
            getCommand("skyblockitems").setTabCompleter(sbiCommand);
            getLogger().info("Commands registered.");

            getLogger().info("SkyBlockItems has been enabled successfully!");
        } catch (Throwable e) {
            getLogger().severe("CRITICAL ERROR DURING PLUGIN ENABLE:");
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
        }
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

        // messagesConfig is now managed by ConfigManager
        if (enchantConfigManager != null) {
            messagesConfig = enchantConfigManager.getMessagesConfig();
        }
    }

    public org.bukkit.configuration.file.FileConfiguration getAbilitiesConfig() {
        return abilitiesConfig;
    }

    public org.bukkit.configuration.file.FileConfiguration getMessagesConfig() {
        return messagesConfig;
    }

    public ConfigManager getConfigManager() {
        return enchantConfigManager;
    }

    public EnchantManager getEnchantManager() {
        return enchantManager;
    }

    public CustomEnchantManager getCustomEnchantManager() {
        return customEnchantManager;
    }

    public ChatInputManager getChatInputManager() {
        return chatInputManager;
    }

    public AuraSkillsHook getAuraSkillsHook() {
        return auraSkillsHook;
    }

    public MMOItemsHook getMMOEnchantHook() {
        return mmoEnchantHook;
    }

    public boolean isAuraSkillsEnabled() {
        return auraSkillsEnabled;
    }

    public boolean isMMOItemsEnabled() {
        return mmoItemsEnabled;
    }

    /**
     * Reloads all configuration files (config.yml, abilities.yml, messages.yml)
     */
    public void reloadAllConfigs() {
        reloadConfig(); // Reload config.yml
        if (enchantConfigManager != null)
            enchantConfigManager.reload();
        loadCustomConfigs(); // Reload abilities.yml and update messagesConfig
        getLogger().info("All configurations reloaded!");
    }
}
