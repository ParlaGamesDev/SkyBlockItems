package dev.agam.skyblockitems.abilities;

import dev.agam.skyblockitems.SkyBlockItems;
import io.lumine.mythic.lib.api.item.NBTItem;

import org.bukkit.ChatColor;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Main listener for handling ability activations.
 * Updated to support MMOItems Set Bonuses via PlayerData.
 */
public class AbilityListener implements Listener {

    private final SkyBlockItems plugin;
    private final AbilityManager abilityManager;

    public AbilityListener() {
        this.plugin = SkyBlockItems.getInstance();
        this.abilityManager = plugin.getAbilityManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK") && !event.getAction().name().contains("LEFT_CLICK"))
            return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        TriggerType trigger;
        if (event.getAction().name().contains("RIGHT_CLICK")) {
            trigger = player.isSneaking() ? TriggerType.SHIFT_RIGHT_CLICK : TriggerType.RIGHT_CLICK;
        } else {
            trigger = player.isSneaking() ? TriggerType.SHIFT_LEFT_CLICK : TriggerType.LEFT_CLICK;
        }

        tryActivateAbilities(player, item, trigger, event);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Player player = null;

        if (event.getDamager() instanceof Player) {
            player = (Player) event.getDamager();
        } else if (event.getDamager() instanceof org.bukkit.entity.Projectile) {
            org.bukkit.entity.Projectile proj = (org.bukkit.entity.Projectile) event.getDamager();
            if (proj.getShooter() instanceof Player) {
                player = (Player) proj.getShooter();
            }
        }

        if (player == null)
            return;

        ItemStack item = player.getInventory().getItemInMainHand();
        tryActivateAbilities(player, item, TriggerType.ON_HIT, event);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();

        // Check Armor (Legacy/NBT check)
        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece == null || armorPiece.getType().isAir())
                continue;
            tryActivateAbilities(player, armorPiece, TriggerType.ON_HIT_TAKEN, event);
        }

        // Check Main Hand (Legacy/NBT check)
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && !main.getType().isAir()) {
            tryActivateAbilities(player, main, TriggerType.ON_HIT_TAKEN, event);
        }

        // Check Stats (Set Bonuses) - This covers Retaliation from Set Bonuses
        checkArmorForSetBonus(player, TriggerType.ON_HIT_TAKEN, event);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        tryActivateAbilities(player, item, TriggerType.ON_BLOCK_BREAK, event);

        // Detect Farming (Crops)
        if (isCrop(event.getBlock().getType())) {
            tryActivateAbilities(player, item, TriggerType.ON_FARM, event);
        }
    }

    private boolean isCrop(org.bukkit.Material material) {
        String name = material.name();
        return name.contains("WHEAT") || name.contains("CARROT") || name.contains("POTATO") ||
                name.contains("BEETROOT") || name.contains("NETHER_WART") || name.contains("COCOA_BEANS");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Arrow))
            return;
        Arrow arrow = (Arrow) event.getEntity();
        if (!(arrow.getShooter() instanceof Player))
            return;
        Player player = (Player) arrow.getShooter();

        ItemStack item = player.getInventory().getItemInMainHand();
        tryActivateAbilities(player, item, TriggerType.ON_ARROW_HIT, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        event.getBlock().setMetadata("PLACED_BY_PLAYER",
                new org.bukkit.metadata.FixedMetadataValue(plugin, true));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityKill(org.bukkit.event.entity.EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        ItemStack item = killer.getInventory().getItemInMainHand();
        tryActivateAbilities(killer, item, TriggerType.ON_KILL, event);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking())
            return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();

        // Item check
        if (item != null && !item.getType().isAir()) {
            tryActivateAbilities(player, item, TriggerType.ON_SNEAK, event);
        }

        // Stat check (Set Bonuses) - for Farmer Aura
        checkArmorForSetBonus(player, TriggerType.ON_SNEAK, event);
    }

    // Check Armor for Set Bonuses (Farmer Aura, Retaliation)
    private void checkArmorForSetBonus(Player player, TriggerType trigger, Event event) {
        for (Map.Entry<String, SkyBlockAbility> entry : abilityManager.getAbilities().entrySet()) {
            String abilityId = entry.getKey();
            SkyBlockAbility ability = entry.getValue();

            if (ability.getDefaultTrigger() != trigger)
                continue;

            // Only specific abilities are treated as "Set Bonuses" requiring full armor
            if (!isSetBonusAbility(abilityId))
                continue;

            String params = getFullSetParams(player, abilityId);
            if (params != null) {
                activateAbility(player, ability, params, event);
            }
        }
    }

    private boolean isSetBonusAbility(String abilityId) {
        String id = abilityId.toUpperCase();
        return id.startsWith("FARMER_AURA") || id.startsWith("RETALIATION");
    }

    private String getFullSetParams(Player player, String abilityId) {
        ItemStack[] armor = player.getInventory().getArmorContents();
        int chunksFound = 0;
        String validParams = null;
        String nbtKey = "SKYBLOCK_" + abilityId.toUpperCase().replace("-", "_");

        for (ItemStack piece : armor) {
            if (piece == null || piece.getType().isAir())
                return null; // Must check all slots, if one is empty -> not full set

            NBTItem nbt = NBTItem.get(piece);
            if (!nbt.hasTag(nbtKey))
                return null; // Missing tag on one piece -> not full set

            // Capture params from the first valid piece (assuming they are uniform)
            if (validParams == null) {
                validParams = nbt.getString(nbtKey);
            }
            chunksFound++;
        }

        return (chunksFound == 4) ? validParams : null;
    }

    private void tryActivateAbilities(Player player, ItemStack item, TriggerType trigger, Event event) {
        NBTItem nbtItem = (item != null && !item.getType().isAir()) ? NBTItem.get(item) : null;
        if (nbtItem != null && !nbtItem.hasType())
            nbtItem = null;

        // WorldGuard Check
        if (!dev.agam.skyblockitems.integration.WorldGuardHook.isAbilitiesEnabled(player, player.getLocation())) {
            return;
        }

        for (Map.Entry<String, SkyBlockAbility> entry : abilityManager.getAbilities().entrySet()) {
            String abilityId = entry.getKey();
            SkyBlockAbility ability = entry.getValue();

            if (ability.getDefaultTrigger() != trigger)
                continue;

            // SPECIAL FIX: Prevent Set Bonus abilities from activating on individual Armor
            // pieces
            // They must ONLY be activated via checkArmorForSetBonus() which enforces the
            // full set
            if (isSetBonusAbility(abilityId) && item != null && isArmor(item.getType())) {
                continue;
            }

            String params = null;

            // 1. Check Item NBT
            if (nbtItem != null) {
                String nbtKey = "SKYBLOCK_" + abilityId.toUpperCase().replace("-", "_");
                if (nbtItem.hasTag(nbtKey)) {
                    params = nbtItem.getString(nbtKey);
                }
            }

            if (params != null && !params.isEmpty()) {
                activateAbility(player, ability, params, event);
            }
        }
    }

    private boolean isArmor(org.bukkit.Material mat) {
        String name = mat.name();
        return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS");
    }

    private void activateAbility(Player player, SkyBlockAbility ability, String paramsStr, Event event) {
        String[] params = paramsStr.trim().split("\\s+");

        // Start with defaults
        double cooldown = ability.getDefaultCooldown();
        double mana = ability.getDefaultManaCost();
        double damage = ability.getDefaultDamage();
        double range = ability.getDefaultRange();

        try {
            if (params.length > 0)
                cooldown = Double.parseDouble(params[0]);
            if (params.length > 1)
                mana = Double.parseDouble(params[1]);
            if (params.length > 2)
                damage = Double.parseDouble(params[2]);
            if (params.length > 3)
                range = Double.parseDouble(params[3]);
        } catch (Exception ignored) {
        }

        if (CooldownManager.isOnCooldown(player.getUniqueId(), ability.getId())) {
            double remaining = CooldownManager.getRemainingCooldown(player.getUniqueId(), ability.getId());
            String msg = plugin.getConfigManager().getMessage("players.cooldown",
                    "{ability}", ability.getDisplayName(),
                    "{remaining}", String.format("%.1f", remaining));
            dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);
            return;
        }

        // 1. Check Level Requirements (MMOItems & AuraSkills)
        if (plugin.isMMOItemsEnabled() && event instanceof PlayerInteractEvent) {
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item != null && !item.getType().isAir()) {
                net.Indyuce.mmoitems.api.player.PlayerData mmoData = net.Indyuce.mmoitems.api.player.PlayerData
                        .get(player);
                net.Indyuce.mmoitems.api.item.mmoitem.MMOItem mmoItem = net.Indyuce.mmoitems.api.item.mmoitem.LiveMMOItem
                        .get(item);
                if (mmoItem != null && !mmoData.getRPG().canUse(mmoItem, true)) {
                    // MMOItems will handle the "Not high enough level" message automatically if
                    // redirected correctly,
                    // but we've already done a basic check.
                    return;
                }
            }
        }

        if (mana > 0) {
            // MANA_EFFICIENCY removed
            if (plugin.isAuraSkillsEnabled()) {
                try {
                    dev.aurelium.auraskills.api.AuraSkillsApi auraApi = dev.aurelium.auraskills.api.AuraSkillsApi.get();
                    dev.aurelium.auraskills.api.user.SkillsUser user = auraApi.getUser(player.getUniqueId());
                    if (user.getMana() < mana) {
                        String msg = plugin.getConfigManager().getMessage("players.not-enough-mana",
                                "{max}", String.valueOf((int) user.getMana()),
                                "{need}", String.valueOf((int) mana));
                        dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);
                        return;
                    }
                    user.setMana(user.getMana() - mana);
                } catch (Exception ignored) {
                }
            }
        }

        // Apply CHRONOS
        int chronosLevel = plugin.getCustomEnchantListener().getEffectiveEnchantLevel(player, "CHRONOS");
        if (chronosLevel > 0) {
            // Rebuilt: Each level gives 10% reduction, max 30% at level 3.
            double reduction = Math.min(0.3, chronosLevel * 0.1);
            cooldown = cooldown * (1.0 - reduction);
        }

        if (ability.activate(player, event, cooldown, mana, damage, range)) {
            if (cooldown > 0) {
                CooldownManager.setCooldown(player.getUniqueId(), ability.getId(), cooldown);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;

        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (dev.agam.skyblockitems.abilities.combat.SkySmashAbility.isFalling(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }

        if (event.getEntity().hasMetadata("NEGATE_FALL_DAMAGE")) {
            event.setCancelled(true);
            event.getEntity().removeMetadata("NEGATE_FALL_DAMAGE", plugin);
        }
    }
}
