package com.pg85.otg.config.settings.preset;

import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.config.settingType.Setting;
import com.pg85.otg.config.settingType.Settings;
import com.pg85.otg.config.settings.ConfigSection;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.constants.settings.ConfigMode;
import lombok.Builder;
import lombok.Getter;

import java.util.Locale;

@Builder
@Getter
public class PresetInfo extends ConfigSection {
    private final ConfigMode settingsMode;
    private final String registryName;
    private final int majorVersion;
    private final int minorVersion;
    private final String author;
    private final String description;
    private final boolean selectableInWorldCreation;

    public static final Setting<ConfigMode> SETTINGS_MODE = Settings.enumSetting(
            "SettingsMode", ConfigMode.WriteAll,
            t -> ((PresetInfo) t).getSettingsMode(),
            "Each time " + Constants.MOD_ID + " reads the config files it can also write to them. With this setting you can change how this behaves. Possible modes:",
            "WriteAll - Auto-update settings from old versions, order them, add comments, reset invalid settings and remove custom comments. (Recommended)",
            "WriteWithoutComments - Same as WriteAll, but removes all comments, both the ones added by OTG and custom ones. Removing comments is a recommended optimization for release versions of presets.",
            "WriteDisable - Doesn't write to the config files. Errors are not corrected, old settings are read but are not corrected. Custom comments won't be removed with this mode."
    );
    public static final Setting<String> AUTHOR = Settings.stringSetting(
            "Author", "Unknown",
            t -> ((PresetInfo) t).getAuthor(),
            "The author of this preset"
    );
    public static final Setting<String> REGISTRY_NAME = Settings.stringSetting(
            "RegistryName", "default",
            t -> ((PresetInfo) t).getRegistryName(),
            "The shortened name for the preset, used in biome resource locations and similar"
    );
    public static final Setting<String> DESCRIPTION = Settings.stringSetting(
            "Description", "No description given",
            t -> ((PresetInfo) t).getDescription(),
            "A short description of this preset"
    );
    public static final Setting<Integer> MAJOR_VERSION = Settings.intSetting(
            "MajorVersion", 0 , 0, Integer.MAX_VALUE,
            t -> ((PresetInfo) t).getMajorVersion(),
            "The preset major version. Increasing the minor version makes the PresetPacker overwrite,",
            "while increasing the major version will make the PresetPacker save a new copy"
    );
    public static final Setting<Integer> MINOR_VERSION = Settings.intSetting(
            "MinorVersion", 0 , 0, Integer.MAX_VALUE,
            t -> ((PresetInfo) t).getMinorVersion(),
            "The preset minor version. Increasing the minor version makes the PresetPacker overwrite,",
            "while increasing the major version will make the PresetPacker save a new copy"
    );

    public static final Setting<Boolean> SELECTABLE_IN_WORLD_CREATION = Settings.booleanSetting(
            "SelectableInWorldCreation", true,
            t -> ((PresetInfo) t).isSelectableInWorldCreation(),
            "Whether this preset should be selectable in the world creation screen"
    );

    public static PresetInfo buildPresetInfo(SettingsMap reader) {
        PresetInfoBuilder presetInfoBuilder = builder();

        presetInfoBuilder.settingsMode(reader.getSetting(SETTINGS_MODE));
        presetInfoBuilder.author(reader.getSetting(AUTHOR));
        presetInfoBuilder.description(reader.getSetting(DESCRIPTION));
        presetInfoBuilder.registryName(reader.getSetting(REGISTRY_NAME));
        presetInfoBuilder.majorVersion(reader.getSetting(MAJOR_VERSION));
        presetInfoBuilder.minorVersion(reader.getSetting(MINOR_VERSION));
        presetInfoBuilder.selectableInWorldCreation(reader.getSetting(SELECTABLE_IN_WORLD_CREATION));

        return presetInfoBuilder.fixSettings().build();
    }

    public static class PresetInfoBuilder {
        private PresetInfoBuilder() {
        }
        public PresetInfoBuilder fixSettings() {
            this.registryName = this.registryName
                    .toLowerCase(Locale.ROOT)
                    .replaceAll(" ", "_")
                    .replaceAll("[^a-z0-9_\\-/.]", "");
            return this;
        }
    }

    @Override
    public String getSectionName() {
        return "Preset Info";
    }
}
