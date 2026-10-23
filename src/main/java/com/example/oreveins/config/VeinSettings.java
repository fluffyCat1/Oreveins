package com.example.oreveins.config;

import com.google.gson.annotations.SerializedName;

/**
 * Settings for a single ore-vein block type, as read from
 * config/oreveins-common.json. Field names match the JSON keys.
 */
public class VeinSettings {
    /**
     * How much ore ONE block of this type contributes to its group.
     * Adjacent blocks of the same type form a group whose total is the sum
     * of all its blocks' amounts - the whole group disappears only when
     * that shared total is used up. (Old configs called this
     * "total_amount" - still accepted when reading.)
     */
    @SerializedName(value = "amount_per_block", alternate = {"total_amount"})
    public int amount_per_block = 1000;

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

    public VeinSettings(int amount_per_block, String drop_item, int min_per_hit, int max_per_hit, int xp_per_hit) {
        this.amount_per_block = amount_per_block;
        this.drop_item = drop_item;
        this.min_per_hit = min_per_hit;
        this.max_per_hit = max_per_hit;
        this.xp_per_hit = xp_per_hit;
    }
}
