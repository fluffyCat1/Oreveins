package com.example.oreveins.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
 
import java.util.List;
 
/**
 * Redstone-controlled block breaker.
 * Ломает блок перед собой (по facing), только пока получает редстоун-сигнал,
 * и переносит добытое в ближайшее хранилище / внутренний буфер / на землю.
 *
 * Логика переноса предмета скопирована 1:1 с идеи Auto Drill из README:
 * 1) вплотную стоящий инвентарь по любой из 6 граней
 * 2) внутренний буфер (9 слотов)
 * 3) выброс на землю, если всё занято
 *
 * Отличия от Auto Drill:
 * - НЕ зависит от Create / KineticBlockEntity, никакого RPM.
 * - Работает только пока level.hasNeighborSignal(pos) == true (есть редстоун-сигнал).
 * - Ломает любой блок перед собой (с базовыми проверками "нельзя ломать"),
 *   а не только жилу OreVein.
 */
public class AutoDrillBlockEntity extends BlockEntity {
 
    // ---- Настройки (можно вынести в конфиг по аналогии с OreVeinConfig) ----
    private static final int BREAK_INTERVAL_TICKS = 20; // раз в секунду при наличии сигнала
    private static final int BUFFER_SIZE = 9;
 
    private int cooldown = 0;
 
    private final ItemStackHandler buffer = new ItemStackHandler(BUFFER_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
 
    public RedstoneBreakerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
 
    public ItemStackHandler getBuffer() {
        return buffer;
    }
 
    // ---- Тикер ----
    public static <T extends BlockEntity> BlockEntityTicker<T> getTicker() {
        return (level, pos, state, be) -> {
            if (be instanceof RedstoneBreakerBlockEntity breaker) {
                breaker.tick(level, pos, state);
            }
        };
    }
 
    private void tick(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide) return;
        if (!(level instanceof ServerLevel serverLevel)) return;
 
        // Работаем ТОЛЬКО пока подан редстоун-сигнал. Без сигнала — простой,
        // как и просили: "начинал ломать лишь тогда, когда ему дают редстоун сигнал".
        if (!level.hasNeighborSignal(pos)) {
            cooldown = 0;
            return;
        }
 
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        cooldown = BREAK_INTERVAL_TICKS;
 
        Direction facing = state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING);
        BlockPos targetPos = pos.relative(facing);
        BlockState targetState = level.getBlockState(targetPos);
 
        if (!canBreak(serverLevel, targetPos, targetState)) {
            return;
        }
 
        // Получаем дропы блока БЕЗ реального spawn-а в мире, чтобы сразу
        // распределить их по хранилищам (как у Auto Drill).
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                targetState, serverLevel, targetPos,
                serverLevel.getBlockEntity(targetPos)
        );
 
        serverLevel.destroyBlock(targetPos, false); // false = сами распорядимся дропом
        serverLevel.levelEvent(2001, targetPos, net.minecraft.world.level.block.Block.getId(targetState)); // звук/партиклы разрушения
 
        for (ItemStack stack : drops) {
            distribute(level, pos, stack);
        }
    }
 
    private boolean canBreak(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir()) return false;
        if (state.getDestroySpeed(level, pos) < 0) return false; // unbreakable (bedrock и т.п.)
        if (state.getBlock() == Blocks.BEDROCK) return false;
        // По желанию можно добавить тег-блэклист/whitelist, проверку на BlockEntity (сундуки и т.п. не ломать)
        if (level.getBlockEntity(pos) != null) return false;
        return true;
    }
 
    /**
     * Перенос предмета: сначала вплотную стоящее хранилище по любой из 6 граней,
     * затем внутренний буфер, затем — на землю.
     */
    private void distribute(Level level, BlockPos selfPos, ItemStack stack) {
        ItemStack remaining = stack.copy();
 
        for (Direction dir : Direction.values()) {
            if (remaining.isEmpty()) return;
            BlockPos neighborPos = selfPos.relative(dir);
            BlockEntity neighborBe = level.getBlockEntity(neighborPos);
            if (neighborBe == null) continue;
 
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (handler == null) continue;
 
            remaining = ItemHandlerHelper.insertItemStacked(handler, remaining, false);
        }
 
        if (!remaining.isEmpty()) {
            remaining = ItemHandlerHelper.insertItemStacked(buffer, remaining, false);
        }
 
        if (!remaining.isEmpty()) {
            // Буфер тоже полон — выбрасываем на землю перед блоком, чтобы не терять предмет.
            ItemEntity entity = new ItemEntity(level,
                    selfPos.getX() + 0.5, selfPos.getY() + 0.5, selfPos.getZ() + 0.5,
                    remaining);
            level.addFreshEntity(entity);
        }
    }
}
