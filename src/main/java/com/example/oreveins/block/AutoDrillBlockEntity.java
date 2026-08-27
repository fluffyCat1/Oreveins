package com.example.oreveins.block;

import com.example.oreveins.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
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
 * Ticks down while it has a redstone signal; mining now takes
 * {@link #MINE_DURATION_TICKS} ticks of visible progress (cracking texture
 * overlay, same as a player slowly mining) instead of happening instantly,
 * so there's something to see while it works. Once progress reaches 100%
 * it actually mines the block below (reusing the exact same vein-node-aware
 * drop logic as everything else - if it's one of our ore veins,
 * {@link OreNodeBlock}'s own getDrops/onRemove handling kicks in
 * automatically and it only takes a partial "hit", same as being mined by
 * hand).
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
    /** How long (in ticks) a single mining cycle takes, cracks-overlay included. 40 ticks = 2s. */
    private static final int MINE_DURATION_TICKS = 40;

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

    private int progress = 0;
    /** A stable "breaker id" for the crack-overlay packets - just needs to be unique-ish per block position. */
    private final int breakerId;

    public AutoDrillBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AUTO_DRILL.get(), pos, state);
        this.breakerId = pos.hashCode();
    }

    public IItemHandler getItemHandler() {
        return inventory;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AutoDrillBlockEntity be) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        BlockPos targetPos = pos.below();

        if (!level.hasNeighborSignal(pos)) {
            // No power: stop and clear any half-finished cracks.
            if (be.progress > 0) {
                be.progress = 0;
                serverLevel.destroyBlockProgress(be.breakerId, targetPos, -1);
            }
            return;
        }

        BlockState targetState = serverLevel.getBlockState(targetPos);
        if (targetState.isAir() || targetState.getDestroySpeed(serverLevel, targetPos) < 0) {
            if (be.progress > 0) {
                be.progress = 0;
                serverLevel.destroyBlockProgress(be.breakerId, targetPos, -1);
            }
            return; // nothing to mine, or unbreakable (bedrock etc.)
        }

        be.progress++;

        int stage = Mth.clamp((be.progress * 10) / MINE_DURATION_TICKS, 0, 9);
        serverLevel.destroyBlockProgress(be.breakerId, targetPos, stage);

        // A couple of hit "clinks" while it works, like a player swinging a tool.
        if (be.progress % 8 == 0) {
            serverLevel.playSound(null, targetPos, SoundEvents.STONE_HIT, SoundSource.BLOCKS, 0.3f, 1.0f);
        }

        if (be.progress < MINE_DURATION_TICKS) {
            return;
        }

        // Finished this cycle: clear the cracks, actually mine, and start over.
        be.progress = 0;
        serverLevel.destroyBlockProgress(be.breakerId, targetPos, -1);

        BlockEntity targetBe = targetState.hasBlockEntity() ? serverLevel.getBlockEntity(targetPos) : null;
        List<ItemStack> drops = Block.getDrops(targetState, serverLevel, targetPos, targetBe);

        // Actually remove/replace the block. For our own ore-vein nodes this
        // triggers their own getDrops()/onRemove() handling (see
        // OreNodeBlock), which will have already been used above to compute
        // "drops" and will put the node back with a reduced amount unless
        // it's now empty - exactly like being hit by hand.
        serverLevel.removeBlock(targetPos, false);

        // Break particles + sound, same visual "poof" as a normal break.
        serverLevel.levelEvent(2001, targetPos, Block.getId(targetState));

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
        tag.putInt("Progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Progress")) {
            progress = tag.getInt("Progress");
        }
    }
}
