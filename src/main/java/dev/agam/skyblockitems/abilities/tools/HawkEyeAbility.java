package dev.agam.skyblockitems.abilities.tools;

import dev.agam.skyblockitems.SkyBlockItems;
import dev.agam.skyblockitems.abilities.SkyBlockAbility;
import dev.agam.skyblockitems.abilities.TriggerType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class HawkEyeAbility extends SkyBlockAbility implements Listener {

    private static final Map<UUID, Map<Block, Entity>> playerGlowingBlocks = new HashMap<>();

    public HawkEyeAbility() {
        super("HAWK_EYE", "ראיית נץ", TriggerType.RIGHT_CLICK, 60.0, 30.0, 0.0, 10.0);
        Bukkit.getPluginManager().registerEvents(this, SkyBlockItems.getInstance());
    }

    @Override
    public boolean activate(Player player, Event event, double cooldown, double manaCost, double damage, double range) {
        io.lumine.mythic.lib.api.item.NBTItem nbtItem = io.lumine.mythic.lib.api.item.NBTItem
                .get(player.getInventory().getItemInMainHand());
        String value = nbtItem.getString("SKYBLOCK_HAWK_EYE");
        if (value == null || value.isEmpty())
            return false;

        String[] params = value.split("\\s+");
        if (params.length < 5)
            return false;

        Material targetMat;
        try {
            targetMat = Material.valueOf(params[2].toUpperCase());
        } catch (Exception e) {
            targetMat = Material.DIAMOND_ORE;
        }

        int radius = Math.min(15, (int) Double.parseDouble(params[3]));
        int durationSeconds = (int) Double.parseDouble(params[4]);

        Block center = player.getLocation().getBlock();
        boolean found = false;

        // Optimization: Use a bounding box iterator or limit the scope
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block b = center.getRelative(x, y, z);
                    if (b.getType() == targetMat) {
                        highlightBlock(player, b, durationSeconds);
                        found = true;
                    }
                }
            }
        }

        if (found) {
            player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, 1.0f, 1.0f);
        }

        return true;
    }

    private void highlightBlock(Player activator, Block block, int durationSeconds) {
        Map<Block, Entity> playerMap = playerGlowingBlocks.computeIfAbsent(activator.getUniqueId(),
                k -> new HashMap<>());

        if (playerMap.containsKey(block))
            return;

        // Use a Slime instead of Shulker because it has a smaller hitbox or can be
        // sized
        // Small slimes with AI disabled have almost no collision interference with
        // mining
        Shulker shulker = block.getWorld().spawn(block.getLocation().add(0.5, 0, 0.5), Shulker.class, s -> {
            s.setAI(false);
            s.setInvulnerable(true);
            s.setSilent(true);
            s.setInvisible(true);
            s.setGlowing(true);
            s.setPersistent(false);
            // Team with no collision
            org.bukkit.scoreboard.Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
            org.bukkit.scoreboard.Team team = sb.getTeam("SBI_HAWKEYE");
            if (team == null) {
                team = sb.registerNewTeam("SBI_HAWKEYE");
                team.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE,
                        org.bukkit.scoreboard.Team.OptionStatus.NEVER);
            }
            team.addEntry(s.getUniqueId().toString());
        });

        // Make it only visible to the activator
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getUniqueId().equals(activator.getUniqueId())) {
                p.hideEntity(SkyBlockItems.getInstance(), shulker);
            }
        }

        playerMap.put(block, shulker);

        // Remove after duration
        Bukkit.getScheduler().runTaskLater(SkyBlockItems.getInstance(), () -> {
            removeGlow(activator.getUniqueId(), block);
        }, durationSeconds * 20L);
    }

    private void removeGlow(UUID playerUUID, Block block) {
        Map<Block, Entity> playerMap = playerGlowingBlocks.get(playerUUID);
        if (playerMap != null) {
            Entity e = playerMap.remove(block);
            if (e != null && e.isValid()) {
                e.remove();
            }
            if (playerMap.isEmpty()) {
                playerGlowingBlocks.remove(playerUUID);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        // Check all players' glowing blocks
        for (UUID uuid : new HashSet<>(playerGlowingBlocks.keySet())) {
            removeGlow(uuid, block);
        }
    }

    @Override
    public List<String> getLore(double cooldown, double manaCost, double damage, double range, TriggerType trigger) {
        return new ArrayList<>();
    }
}
