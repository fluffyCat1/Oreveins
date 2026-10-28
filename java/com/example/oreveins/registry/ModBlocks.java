package com.example.oreveins.registry;

import com.example.oreveins.OreVeinType;
import com.example.oreveins.OreVeinsMod;
import com.example.oreveins.block.AutoDrillBlock;
import com.example.oreveins.block.OreNodeBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(OreVeinsMod.MOD_ID);

    private static final Map<OreVeinType, DeferredBlock<OreNodeBlock>> NODES = new EnumMap<>(OreVeinType.class);

    public static final DeferredBlock<AutoDrillBlock> AUTO_DRILL = BLOCKS.register("auto_drill", () ->
            new AutoDrillBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .sound(SoundType.NETHERITE_BLOCK)
                    .strength(3.5f, 6.0f)
                    .requiresCorrectToolForDrops()));

    static {
        for (OreVeinType type : OreVeinType.values()) {
            DeferredBlock<OreNodeBlock> block = BLOCKS.register(type.getRegistryName(), () ->
                    new OreNodeBlock(type, BlockBehaviour.Properties.of()
                            .mapColor(MapColor.STONE)
                            .sound(SoundType.STONE)
                            .strength(type.getHardness(), type.getResistance())
                            .requiresCorrectToolForDrops()));
            NODES.put(type, block);
        }
    }

    public static DeferredBlock<OreNodeBlock> get(OreVeinType type) {
        return NODES.get(type);
    }

    public static Map<OreVeinType, DeferredBlock<OreNodeBlock>> all() {
        return NODES;
    }

    private ModBlocks() {
    }
}
