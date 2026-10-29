package com.example.oreveins.event;

import com.example.oreveins.OreVeinsMod;
import com.example.oreveins.block.OreNodeBlock;

import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import rbasamoyai.createbigcannons.events.ProjectileDamageEvent;

@EventBusSubscriber(modid = OreVeinsMod.MOD_ID)
public final class ExplosionProtectionHandler {

    /**
     * Защита рудных жил от обычных взрывов.
     */
    @SubscribeEvent
    public static void onDetonate(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos ->
                event.getLevel()
                        .getBlockState(pos)
                        .getBlock() instanceof OreNodeBlock
        );
    }

    /**
     * Защита рудных жил от повреждений снарядами
     * Create Big Cannons 5.11.7.
     */
    @SubscribeEvent
    public static void onProjectileDamage(ProjectileDamageEvent event) {
        BlockPos pos = event.getPos();

        if (event.getLevel()
                .getBlockState(pos)
                .getBlock() instanceof OreNodeBlock) {
            event.setCanceled(true);
        }
    }

    private ExplosionProtectionHandler() {
    }
}
