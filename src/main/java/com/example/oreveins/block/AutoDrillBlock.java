package com.example.oreveins.block;


import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.MapColor;
import org.jetbrains.annotations.Nullable;
 
/**
 * Блок, аналогичный по духу Auto Drill из README, но:
 * - без зависимости от Create (не KineticBlock),
 * - ставится "лицом" к тому блоку, который будет ломать (как и раньше),
 * - активируется редстоун-сигналом, а не вращением.
 *
 * Регистрация — по аналогии с тем, как у вас в ModBlocks/ModBlockEntities
 * зарегистрирован oreveins:auto_drill (см. блокстейт/модели там же, их можно
 * переиспользовать или сделать свою простую модель).
 */
public class AutoDrillBlock extends Block implements EntityBlock {
 
    public AutoDrillBlock(Properties properties) {
        super(properties.mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(3.5f));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }
 
    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }
 
    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Ставим "лицом" в сторону, куда смотрел игрок — так же, как Auto Drill.
        return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }
 
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneBreakerBlockEntity(ModBlockEntities.REDSTONE_BREAKER.get(), pos, state); // TODO: заменить на ваш реестр BlockEntity
    }
 
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : RedstoneBreakerBlockEntity.getTicker();
    }
 
    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }
}
