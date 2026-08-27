package com.example.oreveins.event;

import com.example.oreveins.OreVeinType;
import com.example.oreveins.OreVeinsMod;
import com.example.oreveins.block.OreNodeBlock;
import com.example.oreveins.block.OreNodeBlockEntity;
import com.example.oreveins.config.OreVeinConfig;
import com.example.oreveins.config.VeinSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.concurrent.ThreadLocalRandom;

@EventBusSubscriber(modid = OreVeinsMod.MOD_ID)
public final class OreNodeMiningHandler {

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (!(state.getBlock() instanceof OreNodeBlock oreNodeBlock)) {
            return;
        }

        // We take over completely: cancel the vanilla break/drop so the node
        // never disappears on its own and only WE control what comes out.
        event.setCanceled(true);

        Level level = event.getLevel().isClientSide() ? null : (Level) event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return; // only run this logic server-side
        }

        Player player = event.getPlayer();
        BlockPos pos = event.getPos();
        OreVeinType type = oreNodeBlock.getVeinType();
        VeinSettings settings = OreVeinConfig.get(type);

        BlockEntity be = serverLevel.getBlockEntity(pos);
        if (!(be instanceof OreNodeBlockEntity nodeEntity)) {
            return;
        }

        int remaining = nodeEntity.getRemaining();
        if (remaining <= 0) {
            depleteToStone(serverLevel, pos);
            return;
        }

        int hitAmount = randomBetween(settings.min_per_hit, settings.max_per_hit);
        hitAmount = Math.min(hitAmount, remaining);
        if (hitAmount <= 0) {
            hitAmount = 1;
        }

        // Drop the configured item.
        ResourceLocation dropId = ResourceLocation.tryParse(settings.drop_item);
        Item dropItem = dropId != null ? BuiltInRegistries.ITEM.get(dropId) : null;
        if (dropItem != null && dropItem != net.minecraft.world.item.Items.AIR) {
            ItemStack stack = new ItemStack(dropItem, hitAmount);
            net.minecraft.world.level.block.Block.popResource(serverLevel, pos, stack);
        }

        // XP.
        if (settings.xp_per_hit > 0) {
            ExperienceOrb.award(serverLevel, pos.getCenter(), settings.xp_per_hit);
        }

        // Damage the tool like a normal mined block would.
        if (player instanceof ServerPlayer serverPlayer) {
            ItemStack tool = serverPlayer.getMainHandItem();
            if (!tool.isEmpty()) {
                tool.hurtAndBreak(1, serverPlayer, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
            }
        }

        // Feedback: sound + block-break particles, without removing the block.
        serverLevel.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.6f, 1.0f);
        serverLevel.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));

        int newRemaining = remaining - hitAmount;
        nodeEntity.setRemaining(newRemaining);
        serverLevel.sendBlockUpdated(pos, state, state, 3);

        if (newRemaining <= 0) {
            depleteToStone(serverLevel, pos);
        }
    }

    private static void depleteToStone(ServerLevel level, BlockPos pos) {
        level.setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        level.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(Blocks.STONE.defaultBlockState()));
    }

    private static int randomBetween(int min, int max) {
        if (max <= min) {
            return Math.max(min, 1);
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    private OreNodeMiningHandler() {
    }
}
