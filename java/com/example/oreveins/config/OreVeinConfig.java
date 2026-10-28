package com.example.oreveins.config;

import com.example.oreveins.OreVeinType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reads (and, on first run, writes) config/oreveins-common.json.
 *
 * The file maps a vein block id (e.g. "oreveins:iron_ore_vein") to its {@link VeinSettings}. Edit the file, then
 * restart the game/server to apply changes - it is loaded once, early,
 * during mod construction, because block entities need it as soon as a
 * vein node block is placed or loaded.
 */
public final class OreVeinConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<OreVeinType, VeinSettings> SETTINGS = new EnumMap<>(OreVeinType.class);

    private OreVeinConfig() {
    }

    public static void load() {
        Path path = FMLPaths.CONFIGDIR.get().resolve("oreveins-common.json");
        Map<String, VeinSettings> byKey;

        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<LinkedHashMap<String, VeinSettings>>() {}.getType();
                byKey = GSON.fromJson(reader, type);
                if (byKey == null) {
                    byKey = defaults();
                }
            } catch (IOException e) {
                throw new RuntimeException("[oreveins] Could not read " + path, e);
            }
        } else {
            byKey = defaults();
            write(path, byKey);
        }

        // Each entry is looked up by full block id ("oreveins:iron_ore_vein"),
        // falling back to the old short key ("iron") and finally to the
        // built-in default, so the mod never crashes because the user
        // removed/renamed an entry.
        SETTINGS.clear();
        for (OreVeinType type : OreVeinType.values()) {
            VeinSettings found = byKey.get(blockId(type));
            if (found == null) {
                found = byKey.get(type.configKey());
            }
            SETTINGS.put(type, found != null ? found : defaultFor(type));
        }
    }

    private static void write(Path path, Map<String, VeinSettings> data) {
        try {
            Files.createDirectories(path.getParent());
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("[oreveins] Could not write default config to " + path, e);
        }
    }

    private static Map<String, VeinSettings> defaults() {
        Map<String, VeinSettings> map = new LinkedHashMap<>();
        for (OreVeinType type : OreVeinType.values()) {
            map.put(blockId(type), defaultFor(type));
        }
        return map;
    }

    /** Config key = the vein block's full registry id, e.g. "oreveins:iron_ore_vein". */
    public static String blockId(OreVeinType type) {
        return com.example.oreveins.OreVeinsMod.MOD_ID + ":" + type.getRegistryName();
    }

    private static VeinSettings defaultFor(OreVeinType type) {
        return switch (type) {
            case IRON -> new VeinSettings(1000, "minecraft:raw_iron", 1, 3, 0);
            case GOLD -> new VeinSettings(1000, "minecraft:raw_gold", 1, 3, 0);
            case DIAMOND -> new VeinSettings(200, "minecraft:diamond", 1, 1, 2);
            case EMERALD -> new VeinSettings(200, "minecraft:emerald", 1, 1, 2);
            case REDSTONE -> new VeinSettings(1500, "minecraft:redstone", 2, 5, 1);
            case LAPIS -> new VeinSettings(1500, "minecraft:lapis_lazuli", 2, 5, 1);
            case COAL -> new VeinSettings(1500, "minecraft:coal", 2, 5, 0);
            case COPPER -> new VeinSettings(1500, "minecraft:raw_copper", 2, 5, 0);
            case QUARTZ -> new VeinSettings(1500, "minecraft:quartz", 2, 5, 1);
            case ZINC -> new VeinSettings(1000, "create:raw_zinc", 1, 3, 0);
            case ANDESITE -> new VeinSettings(2000, "minecraft:andesite", 3, 8, 0);
            case SULFUR -> new VeinSettings(1200, "oreveins:sulfur_dust", 2, 5, 0);
            case PLATINUM -> new VeinSettings(800, "createpropulsion:raw_platinum", 1, 3, 0);
        };
    }

    public static VeinSettings get(OreVeinType type) {
        VeinSettings settings = SETTINGS.get(type);
        return settings != null ? settings : defaultFor(type);
    }
}
