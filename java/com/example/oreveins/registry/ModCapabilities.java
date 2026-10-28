package com.example.oreveins.registry;

import com.example.oreveins.OreVeinsMod;
import com.example.oreveins.block.AutoDrillBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;

@EventBusSubscriber(modid = OreVeinsMod.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class ModCapabilities {

    @SubscribeEvent
    public static void register(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.AUTO_DRILL.get(),
                (be, side) -> be.getItemHandler()
        );
    }

    private ModCapabilities() {
    }
}
