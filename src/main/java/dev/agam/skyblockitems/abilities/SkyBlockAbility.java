package dev.agam.skyblockitems.abilities;

import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import java.util.List;

public abstract class SkyBlockAbility {

    // ===== Hex Color Constants for consistent styling =====
    public static final String COLOR_MANA = "&#5B9BD5"; // Light Blue
    public static final String COLOR_COOLDOWN = "&#FF6B6B"; // Soft Red
    public static final String COLOR_DAMAGE = "&#FF8C42"; // Orange
    public static final String COLOR_RANGE = "&#7DCE82"; // Soft Green
    public static final String COLOR_DURATION = "&#DDA0DD"; // Light Purple
    public static final String COLOR_GOLD = "&#FFD700"; // Gold
    public static final String COLOR_WHITE = "&f"; // White
    public static final String COLOR_GRAY = "&7"; // Gray
    public static final String COLOR_TITLE = "&#E0E0E0"; // Light Gray for title

    // Additional Standard Colors
    public static final String COLOR_CYAN = "&#00D6FF"; // Aqua
    public static final String COLOR_RED = "&#FF0000"; // Red
    public static final String COLOR_GREEN = "&#20BC24"; // Green
    public static final String COLOR_YELLOW = "&#FFE300"; // Yellow

    private final String id;
    private final String displayName;
    private final TriggerType defaultTrigger;
    private double defaultCooldown;
    private double defaultManaCost;
    private double defaultDamage;
    private double defaultRange;

    public SkyBlockAbility(String id, String displayName, TriggerType defaultTrigger,
            double defaultCooldown, double defaultManaCost, double defaultDamage, double defaultRange) {
        this.id = id;
        this.displayName = displayName;
        this.defaultTrigger = defaultTrigger;
        this.defaultCooldown = defaultCooldown;
        this.defaultManaCost = defaultManaCost;
        this.defaultDamage = defaultDamage;
        this.defaultRange = defaultRange;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public TriggerType getDefaultTrigger() {
        return defaultTrigger;
    }

    public double getDefaultCooldown() {
        return defaultCooldown;
    }

    public double getDefaultManaCost() {
        return defaultManaCost;
    }

    public double getDefaultDamage() {
        return defaultDamage;
    }

    public double getDefaultRange() {
        return defaultRange;
    }

    public void setDefaultCooldown(double defaultCooldown) {
        this.defaultCooldown = defaultCooldown;
    }

    public void setDefaultManaCost(double defaultManaCost) {
        this.defaultManaCost = defaultManaCost;
    }

    public void setDefaultDamage(double defaultDamage) {
        this.defaultDamage = defaultDamage;
    }

    public void setDefaultRange(double defaultRange) {
        this.defaultRange = defaultRange;
    }

    /**
     * The logic to execute when triggered.
     * Values are passed in case they were overridden on the item.
     */
    public abstract boolean activate(Player player, Event event, double cooldown, double manaCost, double damage,
            double range);

    /**
     * Generate the lore lines for this ability.
     */
    public abstract List<String> getLore(double cooldown, double manaCost, double damage, double range,
            TriggerType trigger);

    // ===== Lore Formatting Helpers =====

    /**
     * Format the ability title line.
     * Example: "ראיית נץ | לחיצה ימנית"
     */
    protected String formatTitle(String name, TriggerType trigger) {
        return COLOR_GOLD + name + " " + COLOR_GRAY + "| " + COLOR_TITLE + trigger.getDisplayName();
    }

    /**
     * Format mana cost.
     * Example: "50 מאנה"
     */
    protected String formatMana(double mana) {
        return COLOR_MANA + (int) mana + " מאנה";
    }

    /**
     * Format cooldown.
     * Example: "5 שניות"
     */
    protected String formatCooldown(double cooldown) {
        if (cooldown >= 60) {
            int minutes = (int) (cooldown / 60);
            int seconds = (int) (cooldown % 60);
            if (seconds > 0) {
                return COLOR_COOLDOWN + minutes + " דקות ו-" + seconds + " שניות";
            }
            return COLOR_COOLDOWN + minutes + " דקות";
        }
        return COLOR_COOLDOWN + String.format("%.1f", cooldown) + " שניות";
    }

    /**
     * Format damage.
     * Example: "10 נזק"
     */
    protected String formatDamage(double damage) {
        return COLOR_DAMAGE + String.format("%.1f", damage) + " נזק";
    }

    /**
     * Format range/radius.
     * Example: "רדיוס 10 בלוקים"
     */
    protected String formatRange(double range) {
        return COLOR_RANGE + "רדיוס " + (int) range + " בלוקים";
    }

    /**
     * Format duration.
     * Example: "למשך 5 שניות"
     */
    protected String formatDuration(double seconds) {
        return COLOR_DURATION + "למשך " + (int) seconds + " שניות";
    }

    /**
     * Format standard mana and cooldown line.
     * Example: "עבור 50 מאנה ניתן להשתמש שוב לאחר 5 שניות"
     */
    protected String formatManaAndCooldown(double mana, double cooldown) {
        StringBuilder sb = new StringBuilder();
        if (mana > 0) {
            sb.append(COLOR_WHITE).append("עבור ").append(formatMana(mana));
        }
        if (cooldown > 0) {
            if (sb.length() > 0)
                sb.append(" ");
            sb.append(COLOR_WHITE).append("ניתן להשתמש שוב לאחר ").append(formatCooldown(cooldown));
        }
        return sb.toString();
    }
}
