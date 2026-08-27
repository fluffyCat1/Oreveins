package com.example.oreveins.block;

import com.example.oreveins.OreVeinType;
import com.example.oreveins.config.OreVeinConfig;
import com.example.oreveins.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class OreNodeBlockEntity extends BlockEntity {
    private int remaining;

    public OreNodeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ORE_NODE.get(), pos, state);
        // Fresh node (placed by hand or generated in the world): start full.
        // If this node is being loaded from disk, loadAdditional() below will
        // overwrite this with the saved value right after construction.
        OreVeinType type = state.getBlock() instanceof OreNodeBlock oreNodeBlock ? oreNodeBlock.getVeinType() : null;
        this.remaining = type != null ? OreVeinConfig.get(type).total_amount : 0;
    }

    public int getRemaining() {
        return remaining;
    }

    public void setRemaining(int remaining) {
        this.remaining = Math.max(0, remaining);
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Remaining", remaining);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Remaining")) {
            remaining = tag.getInt("Remaining");
        }
    }
}
