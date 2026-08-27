package com.example.oreveins.block;

import com.example.oreveins.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Ticks down while receiving rotation from Create; the faster it spins, the
 * more often it mines. Whatever comes out of the vein node is:
 *  1) pushed straight into any inventory touching one of the drill's 6
 *     faces (a chest placed right next to it, a Create Passer, a hopper,
 *     etc.) - this is "send it to the nearest storage" in the simplest,
 *     most reliable form: adjacency, exactly like a vanilla hopper decides
 *     where to push;
 *  2) otherwise kept in the drill's own small internal buffer, which is
 *     exposed as a normal item-handler capability so any hopper/pipe/other
 *     mod's item transport can pull from it later;
 *  3) only if both of those are full does it drop the item on the ground,
 *     so nothing is silently lost.
 *
 * No rotation (speed == 0, e.g. no shaft connected or network stalled) =
 * the drill does nothing and waits.
 */
public class AutoDrillBlockEntity extends KineticBlockEntity {
    private static final int BUFFER_SLOTS = 9;
    /** At 1 RPM the drill mines about once every 6.4s; higher RPM = faster, down to this floor. */
    private static final int MIN_INTERVAL_TICKS = 5;
    private static final int MAX_INTERVAL_TICKS = 128;

    private final ItemStackHandler inventory = new ItemStackHandler(BUFFER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private int cooldown = MAX_INTERVAL_TICKS;

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

        float speed = Math.abs(be.getSpeed());
        if (speed < 1f) {
            // Not receiving rotation (no shaft, or network stalled/overstressed) - idle.
            return;
        }

        if (--be.cooldown > 0) {
            return;
        }
        be.cooldown = Mth.clamp((int) (MAX_INTERVAL_TICKS - speed), MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);

        Direction facing = state.getValue(AutoDrillBlock.FACING);
        BlockPos targetPos = pos.relative(facing);

        ItemStack mined = OreNodeMining.mineOnce(serverLevel, targetPos);
        if (mined.isEmpty()) {
            return;
        }

        ItemStack leftover = pushIntoAdjacentStorage(serverLevel, pos, mined);

        if (!leftover.isEmpty()) {
            leftover = ItemHandlerHelper.insertItemStacked(be.inventory, leftover, false);
        }

        if (!leftover.isEmpty()) {
            Block.popResource(serverLevel, targetPos, leftover);
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
    public void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        tag.put("Inventory", inventory.serializeNBT(registries));
        tag.putInt("Cooldown", cooldown);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("Inventory"));
        }
        if (tag.contains("Cooldown")) {
            cooldown = tag.getInt("Cooldown");
        }
    }
}
