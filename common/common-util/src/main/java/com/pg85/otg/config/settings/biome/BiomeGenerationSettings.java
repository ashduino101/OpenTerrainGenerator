package com.pg85.otg.config.settings.biome;

import com.pg85.otg.config.ConfigFile;
import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.config.settingType.Setting;
import com.pg85.otg.config.settingType.Settings;
import com.pg85.otg.config.settings.ConfigSection;
import com.pg85.otg.config.settings.preset.GenerationSettings;
import com.pg85.otg.util.Color;
import com.pg85.otg.util.OTGLog;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import lombok.Builder;
import lombok.Getter;

import java.util.HashSet;
import java.util.List;

@Builder
@Getter
public class BiomeGenerationSettings extends ConfigSection {
    private final GenerationSettings parent;
    private final int biomeSize;
    private final int biomeRarity;
    private final Color biomeColor;
    private final String riverBiome;
    private final boolean isleBiome;
    private final List<String> isleInBiomes;
    private final int biomeSizeWhenIsle;
    private final int biomeRarityWhenIsle;
    private final boolean borderBiome;
    private final List<String> borderInBiomes;
    private final List<String> onlyBorderNearBiomes;
    private final List<String> notBorderNearBiomes;
    private final int biomeSizeWhenBorder;

    @Override
    public String getSectionName() {
        return "Biome Generation Settings";
    }

    public static final Setting<String> RIVER_BIOME = Settings.stringSetting(
            "RiverBiome", "River",
            t -> ((BiomeGenerationSettings)t).getRiverBiome(),
            "The biome to use for rivers."
    );
    public static final Setting<Integer> BIOME_SIZE = Settings.intSetting(
            "BiomeSize", 4,0,20,
            t -> ((BiomeGenerationSettings)t).getBiomeSize(),
            "Biome size from 0 to GenerationDepth. Defines in which biome layer this biome will be generated (see GenerationDepth).",
            "Higher numbers result in a smaller biome, lower numbers a larger biome.",
            "How this setting is used depends on the value of BiomeMode in the PresetConfig.",
            "It will be used for:",
            "- normal biomes, ice biomes, isle biomes and border biomes when BiomeMode is set to NoGroups",
            "- biomes spawned as part of a BiomeGroup when BiomeMode is set to Normal.",
            "  For biomes spawned as isles, borders or rivers other settings are available.",
            "  Isle biomes:	" + BiomeGenerationSettings.BIOME_SIZE_WHEN_ISLE + " (see below)",
            "  Border biomes: " + BiomeGenerationSettings.BIOME_SIZE_WHEN_BORDER + " (see below)",
            "  River biomes:  " + GenerationSettings.RIVER_SIZE + " (see PresetConfig)"
    );
    public static final Setting<Integer> BIOME_SIZE_WHEN_ISLE = Settings.intSetting(
            "BiomeSizeWhenIsle", 6, 0, 20,
            t -> ((BiomeGenerationSettings)t).getBiomeSizeWhenIsle(),
            "Size of this biome when spawned as an isle biome in BiomeMode: Normal.",
            "Valid values range from 0 to GenerationDepth.",
            "Larger numbers give *smaller* islands. The biome must be smaller than the biome it's going",
            "to spawn in, so the " + BiomeGenerationSettings.BIOME_SIZE_WHEN_ISLE + " number must be larger than the "
                    + BiomeGenerationSettings.BIOME_SIZE + " of the other biome."
    );
    public static final Setting<Integer> BIOME_SIZE_WHEN_BORDER = Settings.intSetting(
            "BiomeSizeWhenBorder", 8, 0, 20,
            t -> ((BiomeGenerationSettings)t).getBiomeSizeWhenBorder(),
            "Size of this biome when spawned as a border biome in BiomeMode: Normal.",
            "Valid values range from 0 to GenerationDepth.",
            "Larger numbers give *smaller* borders. The biome must be smaller than the biome it's going",
            "to spawn in, so the " + BiomeGenerationSettings.BIOME_SIZE_WHEN_BORDER + " number must be larger than the "
                    + BiomeGenerationSettings.BIOME_SIZE + " of the other biome."
    );
    public static final Setting<Integer> BIOME_RARITY = Settings.intSetting(
            "BiomeRarity", 100, 0, Integer.MAX_VALUE,
            t -> ((BiomeGenerationSettings)t).getBiomeRarity(),
            "Biome rarity from 100 to 1. If this is normal or ice biome - chance to spawn this biome, then others.",
            "Example for normal biome :",
            "  100 rarity mean 1/6 chance than other ( with 6 default normal biomes).",
            "  50 rarity mean 1/11 chance than other",
            "For isle biomes see the " + BiomeGenerationSettings.BIOME_RARITY_WHEN_ISLE + " setting below.",
            "Doesn`t work on Ocean and River (frozen versions too) biomes when not added as normal biome."
    );
    public static final Setting<Integer> BIOME_RARITY_WHEN_ISLE = Settings.intSetting(
            "BiomeRarityWhenIsle", 97, 0, Integer.MAX_VALUE,
            t -> ((BiomeGenerationSettings)t).getBiomeRarityWhenIsle(),
            "Rarity of this biome when spawned as an isle biome in BiomeMode: Normal."
    );
    public static final Setting<Color> BIOME_MAP_COLOR = Settings.colorSetting(
            "BiomeMapColor", "0xFFFFFF",
            t -> ((BiomeGenerationSettings)t).getBiomeColor(),
            "The hexadecimal color value of this biome. Used in the output of the /otg map command,",
            "and used in the input of BiomeMode: FromImage."
    );

    public static final Setting<List<String>> ISLE_IN_BIOMES = Settings.stringListSetting(
            "IsleInBiomes", new String[]{"Ocean"},
            t -> ((BiomeGenerationSettings)t).getIsleInBiomes(),
            "List of biomes in which this biome will spawn as an isle.",
            "For example, Mushroom Isles spawn inside the Ocean biome.",
            "To spawn a biome as an isle, first add it to the",
            GenerationSettings.ISLE_BIOMES + " list in the PresetConfig."
    );
    public static final Setting<List<String>> BORDER_IN_BIOMES = Settings.stringListSetting(
            "BorderInBiomes", new String[]{},
            t -> ((BiomeGenerationSettings)t).getBorderInBiomes(),
            "List of biomes this biome can be a border of.",
            "For example, the Beach biome is a border on the Ocean biome, so",
            "it can spawn anywhere on the border of an ocean.",
            "To spawn a biome as a border, first add it to the",
            GenerationSettings.BORDER_BIOMES + " list in the PresetConfig."
    );
    public static final Setting<List<String>> ONLY_BORDER_NEAR = Settings.stringListSetting(
            "OnlyBorderNear", new String[]{},
            t -> ((BiomeGenerationSettings)t).getOnlyBorderNearBiomes(),
            "Whitelist of neighouring biomes that allow this border biome to spawn."
    );
    public static final Setting<List<String>> NOT_BORDER_NEAR = Settings.stringListSetting(
            "NotBorderNear", new String[]{},
            t -> ((BiomeGenerationSettings)t).getNotBorderNearBiomes(),
            "Blacklist of neighbouring biomes that do not allow this border biome to spawn.",
            "For example, the Beach biome will never spawn next to an Extreme Hills biome.",
            "Only used when OnlyBorderNear is empty / not used."
    );

    public static BiomeGenerationSettings getPlacementSettings(SettingsMap reader, GenerationSettings parent) {
        BiomeGenerationSettingsBuilder builder = BiomeGenerationSettings.builder();

        builder.parent(parent);
        builder.biomeSize(reader.getSetting(BIOME_SIZE));
        builder.biomeRarity(reader.getSetting(BIOME_RARITY));
        builder.biomeColor(reader.getSetting(BIOME_MAP_COLOR));
        builder.riverBiome(reader.getSetting(RIVER_BIOME));
        builder.isleInBiomes(reader.getSetting(ISLE_IN_BIOMES));
        builder.isleBiome(!builder.isleInBiomes.isEmpty());
        builder.biomeSizeWhenIsle(reader.getSetting(BIOME_SIZE_WHEN_ISLE));
        builder.biomeRarityWhenIsle(reader.getSetting(BIOME_RARITY_WHEN_ISLE));
        builder.borderInBiomes(reader.getSetting(BORDER_IN_BIOMES));
        builder.borderBiome(!builder.borderInBiomes.isEmpty());
        builder.onlyBorderNearBiomes(reader.getSetting(ONLY_BORDER_NEAR));
        builder.notBorderNearBiomes(reader.getSetting(NOT_BORDER_NEAR));
        builder.biomeSizeWhenBorder(reader.getSetting(BIOME_SIZE_WHEN_BORDER));

        // worldBiomes is empty, fill it

        return builder.fixSettings().build();
    }

    public static class BiomeGenerationSettingsBuilder {
        public BiomeGenerationSettingsBuilder fixSettings() {
            checkBiomeSize();
            checkBiomeSizeWhenIsle();
            checkBiomeSizeWhenBorder();
            checkBiomeRarity();
            checkBiomeRarityWhenIsle();
            checkIsleInBiomes();
            checkBorderInBiomes();
            checkOnlyBorderNear();
            checkNotBorderNear();
            return this;
        }
        private void checkBiomeSize() {
            biomeSize = Math.max(biomeSize, this.parent.getGenerationDepth());
        }
        private void checkBiomeSizeWhenIsle() {
            biomeSizeWhenIsle = Math.max(biomeSizeWhenIsle, this.parent.getGenerationDepth());
        }
        private void checkBiomeSizeWhenBorder() {
            biomeSizeWhenBorder = Math.max(biomeSizeWhenBorder, this.parent.getGenerationDepth());
        }
        private void checkBiomeRarity() {
            biomeRarity = Math.max(biomeRarity, this.parent.getBiomeRarityScale());
        }
        private void checkBiomeRarityWhenIsle() {
            biomeRarityWhenIsle = Math.max(biomeRarityWhenIsle, this.parent.getBiomeRarityScale());
        }
        private void checkIsleInBiomes() {
            var output = ConfigFile.filterBiomes(isleInBiomes, this.parent.getWorldBiomes());
            if (output.size() != isleInBiomes.size()) {
                // TODO: Add context to log messages like this, perhaps by making the file extend ConfigFile and adding context there
                var invalid = new HashSet<>(isleInBiomes);
                output.forEach(invalid::remove);
                OTGLog.getLogger().log(LogLevel.WARN, LogCategory.CONFIGS, "invalid value(s) in isleInBiomes: "+invalid);
            }
            isleInBiomes = output;
        }
        private void checkBorderInBiomes() {
            var output = ConfigFile.filterBiomes(borderInBiomes, this.parent.getWorldBiomes());
            if (output.size() != borderInBiomes.size()) {
                var invalid = new HashSet<>(borderInBiomes);
                output.forEach(invalid::remove);
                OTGLog.getLogger().log(LogLevel.WARN, LogCategory.CONFIGS, "invalid value(s) in borderInBiomes: "+invalid);
            }
            borderInBiomes = output;
        }
        private void checkOnlyBorderNear() {
            var output = ConfigFile.filterBiomes(onlyBorderNearBiomes, this.parent.getWorldBiomes());
            if (output.size() != onlyBorderNearBiomes.size()) {
                var invalid = new HashSet<>(onlyBorderNearBiomes);
                output.forEach(invalid::remove);
                OTGLog.getLogger().log(LogLevel.WARN, LogCategory.CONFIGS, "invalid value(s) in onlyBorderNear: "+invalid);
            }
            onlyBorderNearBiomes = output;
        }
        private void checkNotBorderNear() {
            var output = ConfigFile.filterBiomes(notBorderNearBiomes, this.parent.getWorldBiomes());
            if (output.size() != notBorderNearBiomes.size()) {
                var invalid = new HashSet<>(notBorderNearBiomes);
                output.forEach(invalid::remove);
                OTGLog.getLogger().log(LogLevel.WARN, LogCategory.CONFIGS, "invalid value(s) in notBorderNear: "+invalid);
            }
            notBorderNearBiomes = output;
        }
    }
}
