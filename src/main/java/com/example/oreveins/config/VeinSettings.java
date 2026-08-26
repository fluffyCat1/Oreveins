package com.example.oreveins.config;

/**
 * Settings for a single ore-vein type, as read from
 * config/oreveins-common.json. Field names match the JSON keys (via Gson's
 * default reflective mapping), so don't rename them without updating the
 * default config generator too.
 */
public class VeinSettings {
    /** Total amount of ore this node yields in total before it depletes. */
    public int total_amount = 1000;

    /** Item id that gets dropped, e.g. "minecraft:raw_iron". */
    public String drop_item = "minecraft:iron_ingot";

    /** Minimum amount dropped per successful hit. */
    public int min_per_hit = 1;

    /** Maximum amount dropped per successful hit. */
    public int max_per_hit = 3;

    /** XP orbs' worth given per hit (0 disables XP). */
    public int xp_per_hit = 0;

    public VeinSettings() {
    }

    public VeinSettings(int total_amount, String drop_item, int min_per_hit, int max_per_hit, int xp_per_hit) {
        this.total_amount = total_amount;
        this.drop_item = drop_item;
        this.min_per_hit = min_per_hit;
        this.max_per_hit = max_per_hit;
        this.xp_per_hit = xp_per_hit;
    }
}
