package dev.agam.skyblockitems.enchantsystem.hooks;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.user.SkillsUser;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Hook for AuraSkills integration.
 * Provides mana management and skill level checking.
 */
public class AuraSkillsHook {

    private final AuraSkillsApi api;

    public AuraSkillsHook() {
        this.api = AuraSkillsApi.get();
    }

    /**
     * Get the SkillsUser for a player.
     */
    public SkillsUser getUser(UUID uuid) {
        return api.getUser(uuid);
    }

    /**
     * Get the AuraSkills API instance.
     */
    public AuraSkillsApi getApi() {
        return api;
    }

    /**
     * Get a player's enchanting skill level.
     */
    public int getEnchantingLevel(Player player) {
        try {
            SkillsUser user = api.getUser(player.getUniqueId());
            dev.aurelium.auraskills.api.skill.Skill enchantingSkill = api.getGlobalRegistry().getSkill(
                    dev.aurelium.auraskills.api.registry.NamespacedId.fromDefault("enchanting"));
            if (enchantingSkill != null) {
                return user.getSkillLevel(enchantingSkill);
            }
        } catch (Exception ignored) {
        }
        return 0;
    }
}
