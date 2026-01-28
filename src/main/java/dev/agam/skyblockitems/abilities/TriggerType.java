package dev.agam.skyblockitems.abilities;

public enum TriggerType {
    RIGHT_CLICK("&#FFCF00&lלחיצה ימנית"),
    LEFT_CLICK("&#FFCF00&lלחיצה שמאלית"),
    SHIFT_RIGHT_CLICK("&#FFCF00&lשיפט + לחיצה ימנית"),
    SHIFT_LEFT_CLICK("&#FFCF00&lשיפט + לחיצה שמאלית"),
    ON_HIT("&#FFCF00&lפגיעה באויב"),
    ON_HIT_TAKEN("&#FFCF00&lקבלת פגיעה"),
    ON_KILL("&#FFCF00&lבהריגה"),
    ON_BLOCK_BREAK("&#FFCF00&lחציבה"),
    ON_FARM("&#FFCF00&lעיבוד אדמה / חקלאות"),
    ON_SNEAK("&#FFCF00&lבשיפט"),
    ON_JUMP("&#FFCF00&lבקפיצה"),
    ON_ARROW_HIT("&#FFCF00&lפגיעה עם חץ"),
    SOLAR_STANCE("&#FFCF00&lעמידה בשמש"),
    UNDERWATER("&#FFCF00&lשהייה במים"),
    PASSIVE("&#FFCF00&lיכולת פסיבית"),
    FULL_SET("&#FFCF00&lסט מלא"),
    HOTBAR("&#FFCF00&lשורת הפריטים");

    private final String displayName;

    TriggerType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}