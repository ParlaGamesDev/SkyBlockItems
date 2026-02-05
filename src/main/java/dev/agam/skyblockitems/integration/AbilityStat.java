package dev.agam.skyblockitems.integration;

import io.lumine.mythic.lib.api.item.ItemTag;
import net.Indyuce.mmoitems.api.item.build.ItemStackBuilder;
import net.Indyuce.mmoitems.stat.data.StringData;
import net.Indyuce.mmoitems.stat.type.StringStat;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic MMOItems stat wrapper for SkyBlock abilities.
 * Each ability is registered as a StringStat with Hebrew descriptions.
 */
public class AbilityStat extends StringStat {

    private final String abilityId;
    private final String displayName;

    public AbilityStat(String abilityId, String displayName) {
        super("SKYBLOCK_" + abilityId,
                getMaterialForAbility(abilityId),
                dev.agam.skyblockitems.SkyBlockItems.getInstance().getAbilitiesConfig()
                        .getString("custom-abilities." + abilityId + ".name", displayName),
                generateEditorLore(abilityId, displayName),
                new String[] { "weapon", "tool", "armor", "accessory", "all" });
        this.abilityId = abilityId;
        this.displayName = displayName;
    }

    private static Material getMaterialForAbility(String abilityId) {
        switch (abilityId.toUpperCase()) {
            case "POISON":
                return Material.SPIDER_EYE;
            case "EARTHQUAKE":
                return Material.BROWN_WOOL;
            case "DASH":
                return Material.FEATHER;
            case "HEAL_BEAM":
                return Material.GOLDEN_APPLE;
            case "SKY_SMASH":
                return Material.ELYTRA;
            case "EXPLOSIVE_ARROW":
                return Material.FIREWORK_ROCKET;
            case "LIGHTNING_ARROW":
                return Material.TRIDENT;
            case "HAMMER":
                return Material.IRON_PICKAXE;
            case "TREE_CAPITATOR":
                return Material.DIAMOND_AXE;
            case "MAGNET":
                return Material.HOPPER;
            case "LASER":
                return Material.END_ROD;
            case "HAWK_EYE":
                return Material.SPYGLASS;
            case "LUCKY_TREASURE":
                return Material.EMERALD;
            case "BOOMERANG":
                return Material.TRIDENT;
            case "SOLAR_REPAIR":
                return Material.SUNFLOWER;
            case "DOUBLE_JUMP":
                return Material.RABBIT_FOOT;
            case "FARMING_AOE":
                return Material.BONE_MEAL;
            case "OXYGEN_PURITY":
                return Material.TURTLE_HELMET;
            case "DOG_WHISTLE":
                return Material.BONE;
            case "UNDERWATER_MASTER":
                return Material.HEART_OF_THE_SEA;
            case "RETALIATION_FREEZE":
                return Material.PACKED_ICE;
            case "RETALIATION_LIGHTNING":
                return Material.LIGHTNING_ROD;
            case "WEB_SNARE":
                return Material.COBWEB;
            case "FIRE_ARROW":
                return Material.FLINT_AND_STEEL;
            case "SLOWNESS_ARROW":
                return Material.SLIME_BALL;
            case "MINERS_LUCK":
                return Material.IRON_NUGGET;
            case "THUNDER_STRIKE":
                return Material.GOLDEN_AXE;
            case "CHARCOAL_CONVERTER":
                return Material.CHARCOAL;
            case "BUTCHERS_BLADE":
                return Material.IRON_SWORD;
            case "GOLDEN_LEAF":
                return Material.OAK_LEAVES;
            case "FLIGHT_SKILL":
                return Material.FEATHER;
            case "CROP_OVERDRIVE":
                return Material.BONE_MEAL;
            case "FARMER_AURA_WHEAT":
                return Material.WHEAT;
            case "FARMER_AURA_BEETROOT":
                return Material.BEETROOT;
            case "FARMER_AURA_CARROT":
                return Material.CARROT;
            case "FARMER_AURA_POTATO":
                return Material.POTATO;
            case "DIAMOND_RADAR":
                return Material.DIAMOND;
            case "CLEANSE":
                return Material.MILK_BUCKET;
            case "NIGHT_VISION_CHARM":
                return Material.GOLDEN_CARROT;
            case "FEATHER_WEIGHT":
                return Material.FEATHER;
            case "SLOW_METABOLISM":
                return Material.APPLE;
            case "SPEED":
                return Material.SUGAR;
            case "GRAPPLING_HOOK":
                return Material.FISHING_ROD;
            default:
                return Material.BOOK;
        }
    }

    private static String[] generateEditorLore(String abilityId, String displayName) {
        return new String[] {
                "§7" + getStaticDescription(abilityId),
                "§fפורמט: §bCOOLDOWN MANA " + getParamsGuide(abilityId),
                "",
                "This stat was created by the SkyBlockItems plugin."
        };
    }

    private static String getParamsGuide(String abilityId) {
        switch (abilityId.toUpperCase()) {
            case "POISON":
                return "DURATION AMPLIFIER";
            case "EARTHQUAKE":
                return "RADIUS POWER";
            case "HEAL_BEAM":
                return "HEAL RANGE";
            case "SKY_SMASH":
                return "HEIGHT DAMAGE RADIUS";
            case "EXPLOSIVE_ARROW":
                return "DAMAGE RADIUS";
            case "LIGHTNING_ARROW":
                return "DAMAGE RANGE";
            case "HAMMER":
                return "RADIUS";
            case "TREE_CAPITATOR":
                return "MAX_BLOCKS";
            case "MAGNET":
                return "RADIUS";
            case "LASER":
                return "MAX_DISTANCE";
            case "HAWK_EYE":
                return "BLOCK_TYPE RADIUS DURATION";
            case "LUCKY_TREASURE":
                return "CHANCE MULTIPLIER";
            case "BOOMERANG":
                return "DISTANCE DAMAGE";
            case "DOUBLE_JUMP":
                return "POWER";
            case "OXYGEN_PURITY":
            case "UNDERWATER_MASTER":
                return "LEVEL";
            case "RETALIATION_FREEZE":
                return "DURATION CHANCE";
            case "RETALIATION_LIGHTNING":
                return "DAMAGE CHANCE";
            case "WEB_SNARE":
                return "DURATION";
            case "FIRE_ARROW":
            case "SLOWNESS_ARROW":
                return "DURATION AMPLIFIER";
            case "MINERS_LUCK":
                return "CHANCE AMOUNT";
            case "THUNDER_STRIKE":
                return ""; // No params
            case "CHARCOAL_CONVERTER":
                return ""; // No params
            case "BUTCHERS_BLADE":
                return "CHANCE MULTIPLIER";
            case "GOLDEN_LEAF":
                return "CHANCE AMOUNT";
            case "FLIGHT_SKILL":
                return "DURATION";
            case "CROP_OVERDRIVE":
                return "DURATION";
            case "FARMER_AURA_WHEAT":
            case "FARMER_AURA_BEETROOT":
            case "FARMER_AURA_CARROT":
            case "FARMER_AURA_POTATO":
                return "RADIUS";
            case "DIAMOND_RADAR":
                return "RADIUS";
            case "CLEANSE":
                return ""; // No params
            case "NIGHT_VISION_CHARM":
                return ""; // No params
            case "FEATHER_WEIGHT":
                return "REDUCTION_PERCENT";
            case "SLOW_METABOLISM":
                return "REDUCTION_PERCENT";
            case "SPEED":
                return "DURATION AMPLIFIER";
            case "GRAPPLING_HOOK":
                return "";
            default:
                return "VALUE";
        }
    }

    private static String getStaticDescription(String abilityId) {
        switch (abilityId.toUpperCase()) {
            case "POISON":
                return "מרעיל את המטרה בעת פגיעה.";
            case "EARTHQUAKE":
                return "מרעיד את האדמה ומעיף אויבים.";
            case "HEAL_BEAM":
                return "יורה קרן שמרפאה את השחקן וסביבתו.";
            case "SKY_SMASH":
                return "זינוק לגובה ונחיתה עוצמתית בקרקע.";
            case "EXPLOSIVE_ARROW":
                return "חצים מתפוצצים במגע.";
            case "LIGHTNING_ARROW":
                return "זימון ברק על המטרה בעת פגיעה.";
            case "HAMMER":
                return "חציבה מהירה של שטח רחב.";
            case "TREE_CAPITATOR":
                return "כריתת עצים שלמים בבת אחת.";
            case "MAGNET":
                return "ממגנט אליך שלל שנפל סביבך.";
            case "LASER":
                return "יורה קרן חציבה בקו ישר.";
            case "HAWK_EYE":
                return "מדגיש עפרות נדירות דרך קירות.";
            case "LUCKY_TREASURE":
                return "סיכוי לקבל שלל כפול בחציבה.";
            case "BOOMERANG":
                return "זריקת הפריט שחוזר חזרה ופוגע באויבים.";
            case "DOUBLE_JUMP":
                return "מאפשר קפיצה נוספת באוויר.";
            case "UNDERWATER_MASTER":
                return "מעניק יכולות שחייה ונשימה מתחת למים.";
            case "OXYGEN_PURITY":
                return "נשימה אינסופית מתחת למים.";
            case "DOG_WHISTLE":
                return "משגר אליך מיידית את כל חיות המחמד.";
            case "RETALIATION_FREEZE":
                return "סיכוי להקפיא אויב שתוקף אותך.";
            case "RETALIATION_LIGHTNING":
                return "סיכוי להכות בברק אויב שתוקף אותך.";
            case "WEB_SNARE":
                return "יוצר קורי עכביש בנקודת הפגיעה לעיכוב אויבים.";
            case "FIRE_ARROW":
                return "הופך כל חץ לחץ אש.";
            case "SLOWNESS_ARROW":
                return "הופך כל חץ לחץ האטה.";
            case "MINERS_LUCK":
                return "סיכוי לקבלת נאגטס ברזל מחציבת אבן.";
            case "THUNDER_STRIKE":
                return "ברק שהורס עץ שלם בלחיצה ימנית.";
            case "CHARCOAL_CONVERTER":
                return "הופך עץ לפחם עץ באופן אוטומטי.";
            case "BUTCHERS_BLADE":
                return "מכפיל את כמות הבשר מהריגת חיות.";
            case "GOLDEN_LEAF":
                return "סיכוי להפלת תפוח זהב מחציבת עלים.";
            case "FLIGHT_SKILL":
                return "מעניק יכולת תעופה למשך מספר שניות.";
            case "CROP_OVERDRIVE":
                return "גורם ליבולים להישתל מחדש באופן אוטומטי.";
            case "FARMER_AURA_WHEAT":
                return "הילה שמגדלת חיטה מסביב לשחקן.";
            case "FARMER_AURA_BEETROOT":
                return "הילה שמגדלת סלק מסביב לשחקן.";
            case "FARMER_AURA_CARROT":
                return "הילה שמגדלת גזר מסביב לשחקן.";
            case "FARMER_AURA_POTATO":
                return "הילה שמגדלת תפוחי אדמה מסביב לשחקן.";
            case "DIAMOND_RADAR":
                return "משמיע צפצוף כשיש יהלומים בקרבת מקום.";
            case "CLEANSE":
                return "מסיר את כל האפקטים השליליים מהשחקן.";
            case "NIGHT_VISION_CHARM":
                return "מעניק ראיית לילה קבועה כשהפריט בשימוש.";
            case "FEATHER_WEIGHT":
                return "מפחית את נזק הנפילה מגובה.";
            case "SLOW_METABOLISM":
                return "מפחית את קצב איבוד הרעב של השחקן.";
            case "SPEED":
                return "מעניק אפקט מהירות גבוהה לזמן מוגבל.";
            case "GRAPPLING_HOOK":
                return "משיכה עצמית לנקודת פגיעת החכה.";
            default:
                return "";
        }
    }

    @Override
    public void whenApplied(@NotNull ItemStackBuilder item, @NotNull StringData data) {
        String value = data.toString().trim();
        item.addItemTag(new ItemTag("SKYBLOCK_" + abilityId, value));

        String[] params = value.split("\\s+");

        // Get trigger name from centralized AbilityManager
        String triggerName = "מקש ימני";
        dev.agam.skyblockitems.abilities.SkyBlockAbility ability = dev.agam.skyblockitems.SkyBlockItems.getInstance()
                .getAbilityManager().getAbility(abilityId);
        if (ability != null) {
            triggerName = ability.getDefaultTrigger().getDisplayName();
        }

        double cd = 0;
        int mana = 0;
        try {
            if (params.length >= 1)
                cd = Double.parseDouble(params[0]);
            if (params.length >= 2)
                mana = (int) Double.parseDouble(params[1]);
        } catch (Exception ignored) {
        }

        List<String> loreLines = new ArrayList<>();

        // Check abilities.yml for custom-abilities section
        org.bukkit.configuration.file.FileConfiguration abilitiesConfig = dev.agam.skyblockitems.SkyBlockItems
                .getInstance()
                .getAbilitiesConfig();
        String customPath = null;

        // Try different ID formats
        if (abilitiesConfig.isConfigurationSection("custom-abilities." + abilityId))
            customPath = "custom-abilities." + abilityId;
        else if (abilitiesConfig.isConfigurationSection("custom-abilities." + abilityId.toLowerCase()))
            customPath = "custom-abilities." + abilityId.toLowerCase();
        else if (abilitiesConfig.isConfigurationSection("custom-abilities." + abilityId.toUpperCase()))
            customPath = "custom-abilities." + abilityId.toUpperCase();

        if (customPath != null) {
            // Use config-based lore
            String configDisplayName = abilitiesConfig.getString(customPath + ".name", displayName);
            List<String> configDescription = abilitiesConfig.getStringList(customPath + ".description");

            // Get trigger from config - support both Enum names and custom text (fallback)
            String configTriggerRaw = abilitiesConfig.getString(customPath + ".trigger");
            String configTrigger = triggerName; // Default

            if (configTriggerRaw != null) {
                try {
                    // Try to parse as Enum name (e.g. RIGHT_CLICK)
                    dev.agam.skyblockitems.abilities.TriggerType type = dev.agam.skyblockitems.abilities.TriggerType
                            .valueOf(configTriggerRaw.toUpperCase());
                    configTrigger = type.getDisplayName();
                } catch (IllegalArgumentException e) {
                    // Not an enum name, keep raw string as legacy support or custom text
                    configTrigger = configTriggerRaw;
                }
            }

            // Add Header (from ability-header-format)
            String headerFormat = abilitiesConfig.getString("ability-header-format");
            if (headerFormat != null && !headerFormat.isEmpty()) {
                String formattedHeader = headerFormat
                        .replace("{ability}", configDisplayName)
                        .replace("{trigger}", configTrigger);
                loreLines.add(dev.agam.skyblockitems.utils.ColorUtils.translate(formattedHeader));
            } else {
                // Fallback header if no format defined
                loreLines.add("§6" + configDisplayName + " §7| " + configTrigger);
            }

            // Process description lines with placeholders
            for (String line : configDescription) {
                String processed = line;

                // Replace placeholders with actual values from params
                if (params.length >= 1) {
                    if (cd <= 0 && processed.contains("{cooldown}")) {
                        processed = processed.replaceAll(",?\\s*.*זמן המתנה של.*\\{cooldown\\}.*שניות\\.?", ".");
                        processed = processed.replace("{cooldown}", "0");
                    } else {
                        processed = processed.replace("{cooldown}", params[0]);
                    }
                }
                if (params.length >= 2) {
                    if (mana <= 1.0 && processed.contains("{mana}")) {
                        processed = processed.replaceAll("בעזרת.*\\{mana\\}.*מאנה,?\\s*", "");
                        processed = processed.replace("{mana}", "0");
                    } else {
                        processed = processed.replace("{mana}", params[1]);
                    }
                }
                if (params.length >= 3) {
                    processed = processed.replace("{damage}", params[2]).replace("{radius}", params[2])
                            .replace("{duration}", params[2]).replace("{range}", params[2])
                            .replace("{value}", params[2]);
                }
                if (params.length >= 4) {
                    processed = processed.replace("{power}", params[3]).replace("{amplifier}", params[3]);
                }
                if (params.length >= 5) {
                    processed = processed.replace("{extra}", params[4]);
                }

                // ID Normalization for checking
                String normalizedId = abilityId.replace("-", "_").toUpperCase();

                // Specific Overrides for Solar Repair (Boolean flag style)
                if (normalizedId.equals("SOLAR_REPAIR")) {
                    double configAmount = abilitiesConfig.getDouble("custom-abilities.SOLAR_REPAIR.charge-amount", 1.0);
                    double configMax = abilitiesConfig.getDouble("custom-abilities.SOLAR_REPAIR.max-charge", 120.0);

                    processed = processed.replace("{damage}", String.valueOf(configAmount));
                    processed = processed.replace("{range}", String.valueOf((int) configMax));
                    processed = processed.replace("{charge}", "0");
                }

                // Use ColorUtils for Hex and Legacy support
                String processedTranslated = dev.agam.skyblockitems.utils.ColorUtils.translate(processed);
                String stripped = org.bukkit.ChatColor.stripColor(processedTranslated).trim();

                if (!stripped.isEmpty() && !stripped.equals(".")) {
                    loreLines.add(processedTranslated);
                }
            }
        } else {
            // No config found - show error message
            loreLines.add("§6" + displayName + " §7| §e" + triggerName);
            loreLines.add("§c[SkyBlockItems] הגדרה חסרה בקונפיג: " + abilityId);
        }

        List<String> coloredLore = new ArrayList<>();
        for (String line : loreLines) {
            coloredLore.add(line); // Already translated above
        }

        // Robust insertion: try standard placeholders first, then fallback to append
        boolean inserted = false;

        // Use reflection to safely check for markers/get lines to avoid compile errors
        try {
            Object loreBuilder = item.getLore();
            java.util.List<String> lines = null;

            // Try different ways to get the lines depending on MMOItems version
            try {
                lines = (java.util.List<String>) loreBuilder.getClass().getMethod("getLines").invoke(loreBuilder);
            } catch (Exception e) {
                try {
                    lines = (java.util.List<String>) loreBuilder.getClass().getMethod("getLore").invoke(loreBuilder);
                } catch (Exception e2) {
                    try {
                        java.lang.reflect.Field field = loreBuilder.getClass().getDeclaredField("lore");
                        field.setAccessible(true);
                        lines = (java.util.List<String>) field.get(loreBuilder);
                    } catch (Exception e3) {
                    }
                }
            }

            if (lines != null) {
                // Check for markers manually
                int markerIndex = -1;
                String markerName = null;

                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line.contains("ability-description")) {
                        markerIndex = i;
                        markerName = "ability-description";
                        break;
                    }
                    if (line.contains("abilities") && markerName == null) {
                        markerIndex = i;
                        markerName = "abilities";
                    }
                }

                if (markerName != null) {
                    item.getLore().insert(markerName, coloredLore);
                    inserted = true;
                }

                if (!inserted) {
                    // Fallback: direct addition if markers not found
                    lines.addAll(coloredLore);
                    inserted = true;
                }
            }
        } catch (Exception ignored) {
        }

        if (!inserted) {
            // Final fallback: try standard insertion anyway
            item.getLore().insert("ability-description", coloredLore);
        }
    }
}
