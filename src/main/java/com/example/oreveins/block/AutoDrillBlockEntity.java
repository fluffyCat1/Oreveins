package com.example.oreveins.block;

import com.example.oreveins.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * Ticks down while it has a redstone signal; once the timer hits zero it
 * mines the block directly below it (reusing the exact same
 * vein-node-aware drop logic as everything else - if the block below is
 * one of our ore veins, {@link OreNodeBlock}'s own getDrops/onRemove
 * handling kicks in automatically and it only takes a partial "hit", same
 * as being mined by hand or by Create's drill).
 *
 * The result is:
 *  1) pushed into any inventory touching one of the drill's 6 faces
 *     (nearest storage, in the simple "directly adjacent" sense);
 *  2) otherwise kept in its own 1-slot, up-to-64-item internal buffer,
 *     which any hopper/pipe can pull from (exposes the standard
 *     item-handler capability);
 *  3) if that's also full, dropped on the ground so nothing is lost.
 */
public class AutoDrillBlockEntity extends BlockEntity {
    private static final int INTERVAL_TICKS = 20; // once per second while powered

    private final ItemStackHandler inventory = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }
    };

    private int cooldown = INTERVAL_TICKS;

    public AutoDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_DRILL.get(), pos, state);
    }

    public IItemHandler getItemHandler() {
        return inventory;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoDrillBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (!level.hasNeighborSignal(pos)) {
            return; // no redstone signal - idle
        }

        if (--be.cooldown > 0) {
            return;
        }
        be.cooldown = INTERVAL_TICKS;

        BlockPos targetPos = pos.below();
        BlockState targetState = serverLevel.getBlockState(targetPos);
        if (targetState.isAir() || targetState.getDestroySpeed(serverLevel, targetPos) < 0) {
            return; // nothing to mine, or unbreakable (bedrock etc.)
        }

        BlockEntity targetBe = targetState.hasBlockEntity() ? serverLevel.getBlockEntity(targetPos) : null;
        List<ItemStack> drops = Block.getDrops(targetState, serverLevel, targetPos, targetBe);

        // Actually remove/replace the block. For our own ore-vein nodes this
        // triggers their own getDrops()/onRemove() handling (see
        // OreNodeBlock), which will have already been used above to compute
        // "drops" and will put the node back with a reduced amount unless
        // it's now empty - exactly like being hit by hand or by a machine.
        serverLevel.removeBlock(targetPos, false);

        for (ItemStack drop : drops) {
            ItemStack leftover = pushIntoAdjacentStorage(serverLevel, pos, drop);
            if (!leftover.isEmpty()) {
                leftover = ItemHandlerHelper.insertItemStacked(be.inventory, leftover, false);
            }
            if (!leftover.isEmpty()) {
                Block.popResource(serverLevel, targetPos, leftover);
            }
        }
    }

    private static ItemStack pushIntoAdjacentStorage(ServerLevel level, BlockPos pos, ItemStack stack) {
        for (Direction dir : Direction.values()) {
            if (stack.isEmpty()) {
                break;
            }
            BlockPos neighborPos = pos.relative(dir);
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, dir.getOpposite());
            if (handler != null) {
                stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);
            }
        }
        return stack;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Cooldown")) {
            cooldown = tag.getInt("Cooldown");
        }
    }
}
