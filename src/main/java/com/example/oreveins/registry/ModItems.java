package com.example.oreveins.registry;

import com.example.oreveins.OreVeinType;
import com.example.oreveins.OreVeinsMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(OreVeinsMod.MOD_ID);

    private static final Map<OreVeinType, DeferredItem<BlockItem>> NODE_ITEMS = new EnumMap<>(OreVeinType.class);

    public static final DeferredItem<BlockItem> AUTO_DRILL_ITEM =
            ITEMS.registerSimpleBlockItem("auto_drill", ModBlocks.AUTO_DRILL);

    static {
        for (OreVeinType type : OreVeinType.values()) {
            DeferredItem<BlockItem> item = ITEMS.registerSimpleBlockItem(
                    type.getRegistryName(), ModBlocks.get(type));
            NODE_ITEMS.put(type, item);
        }
    }

    public static DeferredItem<BlockItem> get(OreVeinType type) {
        return NODE_ITEMS.get(type);
    }

    private ModItems() {
    }
}
