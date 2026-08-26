package com.example.oreveins;

import com.example.oreveins.config.OreVeinConfig;
import com.example.oreveins.registry.ModBlockEntities;
import com.example.oreveins.registry.ModBlocks;
import com.example.oreveins.registry.ModCreativeTab;
import com.example.oreveins.registry.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(OreVeinsMod.MOD_ID)
public class OreVeinsMod {
    public static final String MOD_ID = "oreveins";

    public OreVeinsMod(IEventBus modEventBus) {
        // Config is read/created FIRST: block entities read total_amount as
        // soon as they're constructed, which can happen very early (e.g.
        // during world generation on server start).
        OreVeinConfig.load();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModCreativeTab.TABS.register(modEventBus);
    }
}
