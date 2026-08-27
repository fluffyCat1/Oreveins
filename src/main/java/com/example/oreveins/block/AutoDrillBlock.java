package com.example.oreveins.block;

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
 * A simple iron-block-looking machine: give it a redstone signal and it
 * mines the block directly beneath it, on a timer, for as long as it stays
 * powered. Whatever it mines gets pushed into an adjacent inventory if one
 * touches it, otherwise kept in its own small buffer (up to one stack).
 *
 * No Create dependency needed for this - just vanilla redstone.
 */
public class AutoDrillBlock extends Block implements EntityBlock {

    public AutoDrillBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoDrillBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        return createTickerHelper(type, com.example.oreveins.registry.ModBlockEntities.AUTO_DRILL.get(), AutoDrillBlockEntity::serverTick);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<E> createTickerHelper(
            BlockEntityType<E> actualType, BlockEntityType<A> expectedType, BlockEntityTicker<? super A> ticker) {
        return expectedType == actualType ? (BlockEntityTicker<E>) ticker : null;
    }
}
