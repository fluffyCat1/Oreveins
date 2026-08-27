package com.example.oreveins.registry;

import com.example.oreveins.OreVeinType;
import com.example.oreveins.OreVeinsMod;
import com.example.oreveins.block.AutoDrillBlockEntity;
import com.example.oreveins.block.OreNodeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(net.minecraft.core.registries.Registries.BLOCK_ENTITY_TYPE, OreVeinsMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OreNodeBlockEntity>> ORE_NODE =
            BLOCK_ENTITIES.register("ore_node", () -> BlockEntityType.Builder.of(
                    OreNodeBlockEntity::new,
                    ModBlocks.all().values().stream()
                            .map(b -> (net.minecraft.world.level.block.Block) b.get())
                            .toArray(net.minecraft.world.level.block.Block[]::new)
            ).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AutoDrillBlockEntity>> AUTO_DRILL =
            BLOCK_ENTITIES.register("auto_drill", () -> BlockEntityType.Builder.of(
                    AutoDrillBlockEntity::new,
                    ModBlocks.AUTO_DRILL.get()
            ).build(null));

    private ModBlockEntities() {
    }
}
