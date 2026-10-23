package com.example.oreveins.block;

import com.example.oreveins.OreVeinType;
import com.example.oreveins.config.OreVeinConfig;
import com.example.oreveins.config.VeinSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A block that behaves like a small "resource node": mining it doesn't
 * remove it. Instead each hit pulls a configurable amount of ore out of it,
 * and only once its stored amount hits zero does it actually break, turning
 * into stone.
 *
 * There are two independent break paths handled here:
 *  1. Player breaks (pickaxe in hand): intercepted and fully cancelled in
 *     {@link com.example.oreveins.event.OreNodeMiningHandler} BEFORE the
 *     engine ever computes drops or removes the block, so nothing below
 *     runs for that case.
 *  2. Everything else (Create's Mechanical Drill, TNT, and any other mod
 *     that destroys blocks directly via Level#destroyBlock instead of
 *     going through the player-break event - this is confirmed to be how
 *     Create's drill works, it does not fire block-break events). For
 *     these, {@link #getDrops} and {@link #onRemove} below intercept it:
 *     getDrops computes a single partial "hit" (same amounts as a player
 *     hit) and records what should happen next; onRemove then - once the
 *     engine has already turned the block to air - immediately schedules
 *     putting the ore-node block back (with the reduced remaining amount)
 *     or turning it into stone if it's now empty. This causes a very brief
 *     (one server tick) flicker for non-player breaks, which is an
 *     unavoidable trade-off given the engine doesn't provide a cancellable
 *     hook for this kind of external destruction.
 */
public class OreNodeBlock extends Block implements EntityBlock {
    private final OreVeinType veinType;

    private static final Map<BlockPos, PendingRespawn> PENDING = new ConcurrentHashMap<>();

    public OreNodeBlock(OreVeinType veinType, Properties properties) {
        super(properties);
        this.veinType = veinType;
    }

    public OreVeinType getVeinType() {
        return veinType;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OreNodeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null; // no per-tick logic needed, everything happens on hit
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        Level level = params.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return Collections.emptyList();
        }
        BlockPos pos = BlockPos.containing(params.getParameter(LootContextParams.ORIGIN));

        // One "hit" taken from the shared pool of the whole vein group.
        VeinGroup.HitResult result = VeinGroup.hit(serverLevel, pos, this);

        if (result.depleted()) {
            // Pool is empty: this block becomes stone (see onRemove) and so
            // does the rest of the group, right now.
            PENDING.put(pos.immutable(), PendingRespawn.stone());
            VeinGroup.clearGroup(serverLevel, result.group(), pos);
        } else {
            PENDING.put(pos.immutable(), PendingRespawn.ore(result.hitBlockRemaining()));
        }

        if (result.mined() <= 0) {
            return Collections.emptyList();
        }

        VeinSettings settings = OreVeinConfig.get(veinType);
        if (settings.xp_per_hit > 0) {
            ExperienceOrb.award(serverLevel, pos.getCenter(), settings.xp_per_hit);
        }

        ItemStack drop = VeinGroup.makeDrop(this, result.mined());
        return drop.isEmpty() ? Collections.emptyList() : List.of(drop);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        PendingRespawn pending = PENDING.remove(pos.immutable());
        super.onRemove(state, level, pos, newState, movedByPiston);

        if (pending != null && !level.isClientSide() && level instanceof ServerLevel serverLevel) {
            BlockPos posCopy = pos.immutable();
            serverLevel.getServer().execute(() -> {
                if (pending.placeOre()) {
                    serverLevel.setBlockAndUpdate(posCopy, state);
                    BlockEntity newBe = serverLevel.getBlockEntity(posCopy);
                    if (newBe instanceof OreNodeBlockEntity node) {
                        node.setRemaining(pending.remaining());
                    }
                } else {
                    serverLevel.setBlockAndUpdate(posCopy, Blocks.STONE.defaultBlockState());
                }
            });
        }
    }

    private record PendingRespawn(boolean placeOre, int remaining) {
        static PendingRespawn ore(int remaining) {
            return new PendingRespawn(true, remaining);
        }

        static PendingRespawn stone() {
            return new PendingRespawn(false, 0);
        }
    }
}
