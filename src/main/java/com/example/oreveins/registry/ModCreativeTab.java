package com.example.oreveins.registry;

import com.example.oreveins.OreVeinType;
import com.example.oreveins.OreVeinsMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTab {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, OreVeinsMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ORE_VEINS_TAB = TABS.register("ore_veins",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.oreveins"))
                    .icon(() -> ModItems.get(OreVeinType.DIAMOND).toStack())
                    .displayItems((params, output) -> {
                        output.accept(ModItems.AUTO_DRILL_ITEM.toStack());
                        for (OreVeinType type : OreVeinType.values()) {
                            output.accept(ModItems.get(type).toStack());
                        }
                    })
                    .withTabsBefore(CreativeModeTabs.COMBAT)
                    .build());

    private ModCreativeTab() {
    }
}
