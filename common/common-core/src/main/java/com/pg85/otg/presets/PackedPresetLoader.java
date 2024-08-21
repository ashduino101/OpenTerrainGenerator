package com.pg85.otg.presets;

import com.pg85.otg.OTG;
import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.io.NameTable;
import com.pg85.otg.config.io.PackedFileSettings;
import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.config.world.WorldConfig;
import com.pg85.otg.customobject.CustomObject;
import com.pg85.otg.exceptions.InvalidConfigException;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A loader for packed OTG presets.
 */
public class PackedPresetLoader
{
    // Keep this in sync with PresetPacker - we should always be able to load the presets we pack
    private static final String magic = "OTG\n";
    private static final int version = 1;

    public static Preset loadPresetFromPack(File file) throws Exception {
        FileInputStream fileStream = new FileInputStream(file);
        DataInputStream stream = new DataInputStream(fileStream);
        FileChannel channel = fileStream.getChannel();

        byte[] magicBuf = new byte[4];
        stream.read(magicBuf, 0, 4);
        String strMagic = new String(magicBuf);
        if (!strMagic.equals(magic)) {
            throw new IOException(String.format("Not a packed OTG preset (found %s)", strMagic));
        }

        int packVersion = stream.readInt();
        if (packVersion > version) {
            throw new IOException(String.format("Pack version too new! Supports up to %d (%d found)", version, packVersion));
        }

        String presetShortName = file.getName().split("\\.(?=[^.]+$)")[0];

        long tocOffset = stream.readLong();

        channel.position(tocOffset);


        NameTable nameTable = NameTable.readFromStream(stream);

        long worldConfigOffset = stream.readLong();
        long mapOffset = stream.readLong();

        int numBiomeConfigs = stream.readInt();
        Map<String, Long> biomeConfigOffsets = new HashMap<>();
        for (int i = 0; i < numBiomeConfigs; i++) {
            biomeConfigOffsets.put(stream.readUTF(), stream.readLong());
        }

        int numBiomeObjects = stream.readInt();
        Map<String, Long> objectOffsets = new HashMap<>();
        for (int i = 0; i < numBiomeObjects; i++) {
            objectOffsets.put(stream.readUTF(), stream.readLong());
        }

        long resourcesOffset = stream.readLong();

        // Load world config
        channel.position(worldConfigOffset);

        SettingsMap worldConfigMap = PackedFileSettings.readFromStream(stream, OTG.getEngine().getLogger(), nameTable);
        WorldConfig worldConfig = new WorldConfig(new File("<packed>").toPath(), worldConfigMap, new ArrayList<>(biomeConfigOffsets.keySet()), OTG.getEngine().getBiomeResourceManager(), OTG.getEngine().getLogger(), OTG.getEngine().getPresetLoader().getMaterialReader(file.getName()), file.getName());
        // Load biome configs
        ArrayList<BiomeConfig> biomeConfigs = new ArrayList<>();
        for (Map.Entry<String, Long> biomeConfigOffset : biomeConfigOffsets.entrySet()) {
            channel.position(biomeConfigOffset.getValue());

            String biomeName = biomeConfigOffset.getKey();
            SettingsMap biomeConfigMap = PackedFileSettings.readFromStream(stream, OTG.getEngine().getLogger(), nameTable);
            BiomeConfig biomeConfig = new BiomeConfig(biomeName, null, file.toPath(), biomeConfigMap, worldConfig, presetShortName, 1, OTG.getEngine().getBiomeResourceManager(), OTG.getEngine().getLogger(), OTG.getEngine().getPresetLoader().getMaterialReader(file.getName()));
            biomeConfigs.add(biomeConfig);
        }

        // Load custom objects
        ArrayList<CustomObject> objects = new ArrayList<>();

        // The file isn't a directory, but that should be fine
        return new Preset(new File(presetShortName).toPath(), presetShortName, worldConfig, biomeConfigs);
    }
}
