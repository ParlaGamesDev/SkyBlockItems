package dev.agam.skyblockitems.enchantsystem.listeners;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.enchantsystem.CustomEnchant;
import dev.agam.skyblockitems.enchantsystem.managers.EnchantManager.EnchantConfig;
import dev.agam.skyblockitems.enchantsystem.utils.ColorUtils;
import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Listener for AuraSkills events.
 * Notifies players when they unlock new enchantments.
 */
public class AuraSkillsListener implements Listener {

    private final SkyBlockItems plugin;

    public AuraSkillsListener(SkyBlockItems plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSkillLevelUp(SkillLevelUpEvent event) {
        try {
            // Check if this is the enchanting skill
            String skillName = event.getSkill().name().toLowerCase();
            if (!skillName.contains("enchanting")) {
                return;
            }

            Player player = event.getPlayer();
            int newLevel = event.getLevel();

            // Check normal enchants
            for (EnchantConfig enchant : plugin.getEnchantManager().getEnchants().values()) {
                int required = plugin.getConfig().getInt("required-enchanting-level-" + enchant.getId().toLowerCase(),
                        enchant.getRequiredEnchantingLevel());

                if (required == newLevel) {
                    notifyUnlock(player, enchant.getDisplayName());
                }
            }

            // Check custom enchants
            for (CustomEnchant enchant : plugin.getCustomEnchantManager().getAllEnchants()) {
                int required = enchant.getRequiredEnchantingLevel();
                if (required == newLevel) {
                    notifyUnlock(player, enchant.getDisplayName());
                }
            }
        } catch (Exception e) {
            // Silently ignore API compatibility issues
        }
    }

    private void notifyUnlock(Player player, String enchantName) {
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        player.sendMessage(plugin.getConfigManager().getMessage("enchanting.unlocked-alert", "{enchant}", enchantName));
    }
}
