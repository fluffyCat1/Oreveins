package com.example.oreveins.block;

import com.example.oreveins.OreVeinType;
import com.example.oreveins.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/**
 * A block that behaves like a small "resource node": mining it doesn't
 * remove it. Instead each hit pulls a configurable amount of ore out of it
 * (see {@link com.example.oreveins.event.OreNodeMiningHandler}), and only
 * once its stored amount hits zero does it actually break, turning into
 * stone.
 */
public class OreNodeBlock extends Block implements EntityBlock {
    private final OreVeinType veinType;

    public OreNodeBlock(OreVeinType veinType, Properties properties) {
        super(properties);
        this.veinType = veinType;
    }

    public OreVeinType getVeinType() {
        return veinType;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreNodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null; // no per-tick logic needed, everything happens on hit
    }
}
