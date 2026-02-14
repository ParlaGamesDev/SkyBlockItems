package dev.agam.skyblockitems.integration;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Integration hook for Vault economy.
 * Provides safe access to economy features with fallback handling.
 */
public class VaultHook {

    private Economy economy;
    private boolean enabled;

    public VaultHook() {
        this.enabled = false;
        setupEconomy();
    }

    /**
     * Attempts to hook into Vault's economy service.
     */
    private void setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            Bukkit.getLogger().info("[SkyBlockItems] Vault not found - economy features disabled");
            return;
        }

        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            Bukkit.getLogger()
                    .warning("[SkyBlockItems] Vault found but no economy provider - economy features disabled");
            return;
        }

        economy = rsp.getProvider();
        enabled = true;
        Bukkit.getLogger().info("[SkyBlockItems] Vault economy hooked successfully");
    }

    /**
     * Checks if Vault economy is available.
     * 
     * @return true if economy is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Checks if a player has enough money.
     * 
     * @param player The player to check
     * @param amount The amount of money required
     * @return true if the player has enough money
     */
    public boolean hasMoney(Player player, double amount) {
        if (!enabled) {
            return true; // If economy is disabled, always pass
        }
        return economy.has(player, amount);
    }

    /**
     * Takes money from a player's account.
     * 
     * @param player The player to take money from
     * @param amount The amount to take
     * @return true if the transaction was successful
     */
    public boolean takeMoney(Player player, double amount) {
        if (!enabled) {
            return true; // If economy is disabled, always succeed
        }

        if (!hasMoney(player, amount)) {
            return false;
        }

        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    /**
     * Gives money to a player's account.
     * 
     * @param player The player to give money to
     * @param amount The amount to give
     * @return true if the transaction was successful
     */
    public boolean depositMoney(Player player, double amount) {
        if (!enabled) {
            return true;
        }
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    /**
     * Gets a player's balance.
     * 
     * @param player The player
     * @return The player's balance, or 0 if economy is disabled
     */
    public double getBalance(Player player) {
        if (!enabled) {
            return 0.0;
        }
        return economy.getBalance(player);
    }

    /**
     * Formats a money amount using the economy's currency format.
     * 
     * @param amount The amount to format
     * @return Formatted string
     */
    public String format(double amount) {
        if (!enabled) {
            return String.valueOf(amount);
        }
        return economy.format(amount);
    }
}
