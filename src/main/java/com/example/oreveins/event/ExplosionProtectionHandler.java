package com.example.oreveins.event;

import com.example.oreveins.OreVeinsMod;
import com.example.oreveins.block.OreNodeBlock;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * Vein blocks are immune to explosions (TNT, creepers, beds in the Nether,
 * etc.): right before an explosion would destroy blocks, any vein block is
 * removed from its list of affected blocks, so it is simply left untouched.
 * Other blocks around it are destroyed as usual.
 */
@EventBusSubscriber(modid = OreVeinsMod.MOD_ID)
public final class ExplosionProtectionHandler {

    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf((BlockPos pos) ->
                event.getLevel().getBlockState(pos).getBlock() instanceof OreNodeBlock);
    }

    private ExplosionProtectionHandler() {
    }
}
