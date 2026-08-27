package com.example.oreveins.block;

import com.example.oreveins.config.OreVeinConfig;
import com.example.oreveins.config.VeinSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.concurrent.ThreadLocalRandom;

/**
 * "Hits" an ore-vein node once and returns whatever it gave out. Used
 * directly by {@link AutoDrillBlockEntity} - unlike the Create-drill
 * compatibility path in {@link OreNodeBlock}, this doesn't need any
 * remove/respawn trickery because WE are the ones calling it, so we can
 * just read/modify the block entity directly without ever actually
 * removing the block (except when it's fully depleted).
 */
public final class OreNodeMining {

    /** Returns ItemStack.EMPTY if the target isn't a vein node, or the node has nothing to give right now. */
    public static ItemStack mineOnce(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof OreNodeBlock oreBlock)) {
            return ItemStack.EMPTY;
        }

        VeinSettings settings = OreVeinConfig.get(oreBlock.getVeinType());
        BlockEntity be = level.getBlockEntity(pos);

        int remaining = settings.total_amount;
        if (be instanceof OreNodeBlockEntity node) {
            remaining = node.getRemaining();
        }

        if (remaining <= 0) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            return ItemStack.EMPTY;
        }

        int hit = randomBetween(settings.min_per_hit, settings.max_per_hit);
        hit = Math.min(hit, remaining);
        if (hit <= 0) {
            hit = 1;
        }
        int newRemaining = remaining - hit;

        if (be instanceof OreNodeBlockEntity node) {
            node.setRemaining(newRemaining);
        }

        if (settings.xp_per_hit > 0) {
            ExperienceOrb.award(level, pos.getCenter(), settings.xp_per_hit);
        }

        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.4f, 1.0f);
        level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));

        if (newRemaining <= 0) {
            level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        }

        ResourceLocation dropId = ResourceLocation.tryParse(settings.drop_item);
        Item dropItem = dropId != null ? BuiltInRegistries.ITEM.get(dropId) : null;
        if (dropItem == null || dropItem == Items.AIR) {
            return ItemStack.EMPTY;
        }
        return new ItemStack(dropItem, hit);
    }

    private static int randomBetween(int min, int max) {
        if (max <= min) {
            return Math.max(min, 1);
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private OreNodeMining() {
    }
}
