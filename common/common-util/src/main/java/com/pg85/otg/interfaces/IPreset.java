package com.pg85.otg.interfaces;

import java.util.ArrayList;

public interface IPreset {

    /** Not expected to be able to handle a different preset format */
    void update(IPreset preset);

    /** This is the preset folder for unpacked presets, and the filename for packed ones */
    String getId();

    String getShortPresetName();

    IWorldConfig getWorldConfig();

    IBiomeConfig getBiomeConfig(String biomeName);

    IMapImageProvider getMapImageSource();

    ArrayList<IBiomeConfig> getAllBiomeConfigs();

    ArrayList<String> getAllBiomeNames();

    int getMajorVersion();
}
