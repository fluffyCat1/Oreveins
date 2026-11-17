package com.example.oreveins.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

/**
 * Окраска травяного слоя (tintindex 0) у блоков-жил по биому.
 * Руда лежит в отдельном слое без tintindex, поэтому её цвет не меняется.
 */
@EventBusSubscriber(modid = "oreveins", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class OreVeinsClient {

    // Блоки, у которых модель использует oreveins:block/ore_vein_grass
    private static final String[] GRASS_VEINS = {
        "iron_ore_vein", "coal_ore_vein", "copper_ore_vein", "gold_ore_vein",
        "diamond_ore_vein", "emerald_ore_vein", "lapis_ore_vein",
        "redstone_ore_vein", "zinc_ore_vein"
    };

    private static List<Block> grassVeins() {
        List<Block> list = new ArrayList<>();
        for (String name : GRASS_VEINS) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("oreveins", name);
            BuiltInRegistries.BLOCK.getOptional(id).ifPresent(list::add);
        }
        return list;
    }

    @SubscribeEvent
    public static void onBlockColors(RegisterColorHandlersEvent.Block event) {
        List<Block> blocks = grassVeins();
        if (blocks.isEmpty()) return;
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) return -1;
            return (level != null && pos != null)
                ? BiomeColors.getAverageGrassColor(level, pos)
                : GrassColor.get(0.5D, 1.0D);
        }, blocks.toArray(new Block[0]));
    }

    @SubscribeEvent
    public static void onItemColors(RegisterColorHandlersEvent.Item event) {
        List<Block> blocks = grassVeins();
        if (blocks.isEmpty()) return;
        event.register((stack, tintIndex) ->
            tintIndex == 0 ? GrassColor.get(0.5D, 1.0D) : -1,
            blocks.toArray(new Block[0]));
    }
}
