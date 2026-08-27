package com.example.oreveins;

/**
 * The set of built-in ore-vein node types. Each one reuses the look, hardness
 * and mining-tool requirement of an existing vanilla ore so we don't need any
 * new textures/models. What DOES change (via config/oreveins-common.json) is:
 *  - the item that drops
 *  - how much total ore the node contains before it depletes
 *  - how much is given out per hit
 *  - how much XP is given per hit
 */
public enum OreVeinType {
    IRON("iron_ore_vein", "minecraft:block/iron_ore", 3.0f, 3.0f, true),
    GOLD("gold_ore_vein", "minecraft:block/gold_ore", 3.0f, 3.0f, true),
    DIAMOND("diamond_ore_vein", "minecraft:block/diamond_ore", 3.0f, 3.0f, true),
    EMERALD("emerald_ore_vein", "minecraft:block/emerald_ore", 3.0f, 3.0f, true),
    REDSTONE("redstone_ore_vein", "minecraft:block/redstone_ore", 3.0f, 3.0f, true),
    LAPIS("lapis_ore_vein", "minecraft:block/lapis_ore", 3.0f, 3.0f, true),
    COAL("coal_ore_vein", "minecraft:block/coal_ore", 3.0f, 3.0f, false),
    COPPER("copper_ore_vein", "minecraft:block/copper_ore", 3.0f, 3.0f, true);

    private final String registryName;
    private final String texture;
    private final float hardness;
    private final float resistance;
    private final boolean requiresCorrectTool;

    OreVeinType(String registryName, String texture, float hardness, float resistance, boolean requiresCorrectTool) {
        this.registryName = registryName;
        this.texture = texture;
        this.hardness = hardness;
        this.resistance = resistance;
        this.requiresCorrectTool = requiresCorrectTool;
    }

    public String getRegistryName() {
        return registryName;
    }

    public String getTexture() {
        return texture;
    }

    public float getHardness() {
        return hardness;
    }

    public float getResistance() {
        return resistance;
    }

    public boolean requiresCorrectTool() {
        return requiresCorrectTool;
    }

    /** The key used in the JSON config file, e.g. "iron". */
    public String configKey() {
        return name().toLowerCase();
    }
}
