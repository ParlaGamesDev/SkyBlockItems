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
import org.bukkit.event.block.Action;
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
 */
public class AbilityListener implements Listener {

    private final AbilityManager abilityManager;

    public AbilityListener() {
        this.abilityManager = SkyBlockItems.getInstance().getAbilityManager();
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK") && !event.getAction().name().contains("LEFT_CLICK"))
            return;

        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir())
            return;

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
        if (item == null || item.getType().isAir())
            return;

        tryActivateAbilities(player, item, TriggerType.ON_HIT, event);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityDamaged(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player))
            return;
        Player player = (Player) event.getEntity();

        for (ItemStack armorPiece : player.getInventory().getArmorContents()) {
            if (armorPiece == null || armorPiece.getType().isAir())
                continue;
            tryActivateAbilities(player, armorPiece, TriggerType.ON_HIT_TAKEN, event);
        }

        // Also check main hand for retaliation shield/sword effects
        ItemStack main = player.getInventory().getItemInMainHand();
        if (main != null && !main.getType().isAir()) {
            tryActivateAbilities(player, main, TriggerType.ON_HIT_TAKEN, event);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir())
            return;

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
        if (item == null || item.getType().isAir())
            return;

        tryActivateAbilities(player, item, TriggerType.ON_ARROW_HIT, event);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockPlace(org.bukkit.event.block.BlockPlaceEvent event) {
        // Tag blocks placed by players to prevent Lucky Treasure dupes
        event.getBlock().setMetadata("PLACED_BY_PLAYER",
                new org.bukkit.metadata.FixedMetadataValue(SkyBlockItems.getInstance(), true));
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onEntityKill(org.bukkit.event.entity.EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null)
            return;

        ItemStack item = killer.getInventory().getItemInMainHand();
        if (item == null || item.getType().isAir())
            return;

        tryActivateAbilities(killer, item, TriggerType.ON_KILL, event);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onSneak(PlayerToggleSneakEvent event) {
        if (!event.isSneaking())
            return;
        Player player = event.getPlayer();
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item != null && !item.getType().isAir()) {
            tryActivateAbilities(player, item, TriggerType.ON_SNEAK, event);
        }
    }

    private void tryActivateAbilities(Player player, ItemStack item, TriggerType trigger, Event event) {
        NBTItem nbtItem = NBTItem.get(item);
        if (nbtItem == null || !nbtItem.hasType())
            return;

        // WorldGuard Check
        if (!dev.agam.skyblockitems.integration.WorldGuardHook.isAbilitiesEnabled(player, player.getLocation())) {
            return;
        }

        for (Map.Entry<String, SkyBlockAbility> entry : abilityManager.getAbilities().entrySet()) {
            String abilityId = entry.getKey();
            SkyBlockAbility ability = entry.getValue();

            String nbtKey = "SKYBLOCK_" + abilityId.toUpperCase();
            if (!nbtItem.hasTag(nbtKey))
                continue;

            String value = nbtItem.getString(nbtKey);
            if (value == null || value.isEmpty())
                continue;

            String[] params = value.trim().split("\\s+");
            double cooldown = 0, mana = 0, damage = 0, range = 0;

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

            if (ability.getDefaultTrigger() != trigger)
                continue;

            if (CooldownManager.isOnCooldown(player.getUniqueId(), abilityId)) {
                double remaining = CooldownManager.getRemainingCooldown(player.getUniqueId(), abilityId);
                String msg = SkyBlockItems.getInstance().getMessagesConfig().getString("players.cooldown",
                        "&cהאביליטי {ability} בטעינה: {remaining} ש'");
                msg = msg.replace("{ability}", ability.getDisplayName()).replace("{remaining}",
                        String.format("%.1f", remaining));
                dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);
                continue;
            }

            // The PvP/NPC check is now handled globally in MMOItemsAbilityListener
            // via MetadataDamageEvent to ensure it works for all abilities and blocks
            // damage/knockback.

            if (mana > 0) {
                try {
                    dev.aurelium.auraskills.api.AuraSkillsApi auraApi = dev.aurelium.auraskills.api.AuraSkillsApi.get();
                    if (auraApi != null) {
                        dev.aurelium.auraskills.api.user.SkillsUser user = auraApi.getUser(player.getUniqueId());
                        if (user.getMana() < mana) {
                            String msg = SkyBlockItems.getInstance().getMessagesConfig().getString("players.no-mana",
                                    "&cאין מספיק מאנה! ({current}/{required})");
                            msg = msg.replace("{current}", String.valueOf((int) user.getMana())).replace("{required}",
                                    String.valueOf((int) mana));
                            dev.agam.skyblockitems.utils.MessageUtils.sendMessage(player, msg);
                            continue;
                        }
                        user.setMana(user.getMana() - mana);
                    }
                } catch (NoClassDefFoundError | Exception ignored) {
                }
            }

            if (ability.activate(player, event, cooldown, mana, damage, range)) {
                if (cooldown > 0) {
                    CooldownManager.setCooldown(player.getUniqueId(), abilityId, cooldown);
                }
            }
        }
    }

    /**
     * Handle fall damage negation (e.g. for SkySmash)
     */
    /**
     * Handle fall damage negation
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onFall(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.FALL)
            return;

        // 1. Handle Player Fall Damage (SkySmash)
        if (event.getEntity() instanceof Player) {
            Player player = (Player) event.getEntity();
            if (dev.agam.skyblockitems.abilities.combat.SkySmashAbility.isFalling(player.getUniqueId())) {
                event.setCancelled(true);
            }
        }

        // 2. Handle Pet Fall Damage (Dog Whistle teleportation)
        if (event.getEntity().hasMetadata("NEGATE_FALL_DAMAGE")) {
            event.setCancelled(true);
            event.getEntity().removeMetadata("NEGATE_FALL_DAMAGE", SkyBlockItems.getInstance());
        }
    }
}
