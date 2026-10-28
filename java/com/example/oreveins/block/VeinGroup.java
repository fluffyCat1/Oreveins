package com.example.oreveins.block;

import com.example.oreveins.config.OreVeinConfig;
import com.example.oreveins.config.VeinSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Adjacent (face-touching) vein blocks of the SAME type form one group that
 * shares a single pool of ore: the sum of every block's remaining amount.
 * Every hit on any block of the group takes ore out of that shared pool,
 * and the blocks only disappear - all at once - when the pool is empty.
 *
 * The group is recomputed on every hit with a flood fill (only through
 * already-loaded chunks, capped at {@link #MAX_GROUP_SIZE} blocks), so
 * there's no extra state to save: each block keeps its own remaining
 * amount in its block entity, and the group total is just their sum.
 */
public final class VeinGroup {
    private static final int MAX_GROUP_SIZE = 4096;

    /**
     * @param mined              how many items this hit yields (0 if the group was already empty)
     * @param hitBlockRemaining  what's left in the block that was hit, after this hit
     * @param depleted           true when the shared pool is now empty and the whole group should vanish
     * @param group              every block of the group (the hit block first)
     */
    public record HitResult(int mined, int hitBlockRemaining, boolean depleted, List<BlockPos> group) {
    }

    /** Takes one "hit" out of the group's shared pool. Does NOT remove any blocks itself. */
    public static HitResult hit(ServerLevel level, BlockPos pos, OreNodeBlock block) {
        VeinSettings settings = OreVeinConfig.get(block.getVeinType());
        List<BlockPos> group = collect(level, pos, block);

        List<OreNodeBlockEntity> nodes = new ArrayList<>(group.size());
        int total = 0;
        for (BlockPos p : group) {
            if (level.getBlockEntity(p) instanceof OreNodeBlockEntity node) {
                nodes.add(node);
                total += node.getRemaining();
            }
        }

        if (total <= 0) {
            return new HitResult(0, 0, true, group);
        }

        int amount = randomBetween(settings.min_per_hit, settings.max_per_hit);
        amount = Math.max(1, Math.min(amount, total));

        // Take from the block that was hit first, then from its neighbours outwards.
        int left = amount;
        for (OreNodeBlockEntity node : nodes) {
            if (left <= 0) {
                break;
            }
            int take = Math.min(left, node.getRemaining());
            if (take > 0) {
                node.setRemaining(node.getRemaining() - take);
                left -= take;
            }
        }

        int hitRemaining = 0;
        if (level.getBlockEntity(pos) instanceof OreNodeBlockEntity hitNode) {
            hitRemaining = hitNode.getRemaining();
        }
        return new HitResult(amount, hitRemaining, total - amount <= 0, group);
    }

    /** Turns every block of the group (except {@code skip}, if given) into stone. */
    public static void clearGroup(ServerLevel level, List<BlockPos> group, BlockPos skip) {
        for (BlockPos p : group) {
            if (p.equals(skip) || !(level.getBlockState(p).getBlock() instanceof OreNodeBlock)) {
                continue;
            }
            level.levelEvent(2001, p, Block.getId(level.getBlockState(p)));
            level.setBlockAndUpdate(p, Blocks.STONE.defaultBlockState());
        }
    }

    /** The configured drop for an amount, or ItemStack.EMPTY if the configured item doesn't exist. */
    public static ItemStack makeDrop(OreNodeBlock block, int amount) {
        VeinSettings settings = OreVeinConfig.get(block.getVeinType());
        ResourceLocation id = ResourceLocation.tryParse(settings.drop_item);
        Item item = id != null ? BuiltInRegistries.ITEM.get(id) : null;
        if (item == null || item == Items.AIR || amount <= 0) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(item, amount);
    }

    /** Flood fill over the 6 face-neighbours, same block only, loaded chunks only. Hit block first. */
    public static List<BlockPos> collect(ServerLevel level, BlockPos start, Block type) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();

        BlockPos first = start.immutable();
        seen.add(first);
        queue.add(first);

        while (!queue.isEmpty() && result.size() < MAX_GROUP_SIZE) {
            BlockPos current = queue.poll();
            result.add(current);
            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (seen.contains(next) || !level.hasChunkAt(next)) {
                    continue;
                }
                if (level.getBlockState(next).is(type)) {
                    seen.add(next);
                    queue.add(next);
                }
            }
        }
        return result;
    }

    private static int randomBetween(int min, int max) {
        if (max <= min) {
            return Math.max(min, 1);
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private VeinGroup() {
    }
}
