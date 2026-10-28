package com.example.oreveins.event;

import com.example.oreveins.OreVeinsMod;
import com.example.oreveins.block.OreNodeBlock;
import com.example.oreveins.block.VeinGroup;
import com.example.oreveins.config.OreVeinConfig;
import com.example.oreveins.config.VeinSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * Player mining of vein blocks. Every hit takes ore out of the vein GROUP's
 * shared pool (see {@link VeinGroup}); the blocks themselves stay put until
 * the pool is empty, then the whole group turns to stone at once.
 */
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
        VeinSettings settings = OreVeinConfig.get(oreNodeBlock.getVeinType());

        VeinGroup.HitResult result = VeinGroup.hit(serverLevel, pos, oreNodeBlock);

        if (result.mined() > 0) {
            ItemStack drop = VeinGroup.makeDrop(oreNodeBlock, result.mined());
            if (!drop.isEmpty()) {
                Block.popResource(serverLevel, pos, drop);
            }

            if (settings.xp_per_hit > 0) {
                ExperienceOrb.award(serverLevel, pos.getCenter(), settings.xp_per_hit);
            }

            // Damage the tool like a normal mined block would.
            if (player instanceof ServerPlayer serverPlayer) {
                ItemStack tool = serverPlayer.getMainHandItem();
                if (!tool.isEmpty()) {
                    tool.hurtAndBreak(1, serverPlayer, EquipmentSlot.MAINHAND);
                }
            }

            // Feedback: sound + block-break particles, without removing the block.
            serverLevel.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.6f, 1.0f);
            serverLevel.levelEvent(2001, pos, Block.getId(state));
        }

        if (result.depleted()) {
            VeinGroup.clearGroup(serverLevel, result.group(), null);
        }
    }

    private OreNodeMiningHandler() {
    }
}
