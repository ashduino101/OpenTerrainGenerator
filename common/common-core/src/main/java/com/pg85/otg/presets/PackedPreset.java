package com.pg85.otg.presets;

import com.pg85.otg.OTG;
import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.util.StringTable;
import com.pg85.otg.config.io.PackedFileSettings;
import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.config.map.ByteArrayMapImageProvider;
import com.pg85.otg.config.world.WorldConfig;
import com.pg85.otg.customobject.bo2.BO2;
import com.pg85.otg.customobject.bo3.BO3;
import com.pg85.otg.customobject.bo3.BO3Config;
import com.pg85.otg.customobject.bo4.BO4;
import com.pg85.otg.customobject.bo4.BO4Config;
import com.pg85.otg.interfaces.*;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import com.pg85.otg.util.materials.MaterialPalette;
import com.pg85.otg.util.nbt.NBTPalette;

import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A packed OTG preset, stored in a compressed binary format.
 */
public class PackedPreset implements IPreset
{
    private static final String magic = PresetPacker.magic;
    // Keep this in sync with PresetPacker - we should always be able to load the presets we pack
    protected static final int version = 2;

    private String id;
    private String shortName;
    private IWorldConfig worldConfig;
    private HashMap<String, IBiomeConfig> biomeConfigs = new HashMap<String, IBiomeConfig>();
    private int majorVersion;
    private IMapImageProvider mapImage;

    private Map<String, Long> objectOffsets;
    private long mapImageOffset;

    MaterialPalette materialPalette;
    NBTPalette metadataPalette;

    DataInputStream stream;
    FileChannel channel;

    public static IPreset loadPresetFromPack(File file) throws Exception {
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


        StringTable nameTable = StringTable.readFromStream(stream);
        MaterialPalette materialPalette = null;
        if (packVersion >= 2) {
            materialPalette = MaterialPalette.readFromStream(stream);
        }

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

        long nbtOffset = stream.readLong();

        // Load world config
        channel.position(worldConfigOffset);

        SettingsMap worldConfigMap = PackedFileSettings.readFromStream(stream, OTG.getEngine().getLogger(), nameTable);
        WorldConfig worldConfig = new WorldConfig(null, worldConfigMap, new ArrayList<>(biomeConfigOffsets.keySet()), OTG.getEngine().getBiomeResourceManager(), OTG.getEngine().getLogger(), OTG.getEngine().getPresetLoader().getMaterialReader(file.getName()), file.getName());
        // Load biome configs
        ArrayList<IBiomeConfig> biomeConfigs = new ArrayList<>();
        for (Map.Entry<String, Long> biomeConfigOffset : biomeConfigOffsets.entrySet()) {
            channel.position(biomeConfigOffset.getValue());

            String biomeName = biomeConfigOffset.getKey();
            SettingsMap biomeConfigMap = PackedFileSettings.readFromStream(stream, OTG.getEngine().getLogger(), nameTable);
            BiomeConfig biomeConfig = new BiomeConfig(biomeName, null, file.toPath(), biomeConfigMap, worldConfig, presetShortName, 1, OTG.getEngine().getBiomeResourceManager(), OTG.getEngine().getLogger(), OTG.getEngine().getPresetLoader().getMaterialReader(presetShortName));
            biomeConfigs.add(biomeConfig);
        }
        // Load object resources
        channel.position(nbtOffset);
        NBTPalette nbtPalette = NBTPalette.readFromStream(stream);

        PackedPreset preset = new PackedPreset(file.getName(), presetShortName, worldConfig, biomeConfigs);

        preset.mapImageOffset = mapOffset;
        preset.objectOffsets = objectOffsets;
        preset.materialPalette = materialPalette;
        preset.metadataPalette = nbtPalette;

        preset.stream = stream;
        preset.channel = channel;

        return preset;
    }

    PackedPreset(String presetId, String presetShortName, IWorldConfig worldConfig, ArrayList<IBiomeConfig> biomeConfigs) {
        this.id = presetId;
        this.shortName = presetShortName;
        this.worldConfig = worldConfig;
        this.majorVersion = worldConfig.getMajorVersion();

        for(IBiomeConfig biomeConfig : biomeConfigs)
        {
            this.biomeConfigs.put(biomeConfig.getName(), biomeConfig);
        }
    }

    @Override
    public void update(IPreset preset) {
        if (!(preset instanceof PackedPreset)) {
            throw new UnsupportedOperationException("Can only update PackedPreset with another PackedPreset");
        }
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getShortPresetName() {
        return this.shortName;
    }

    @Override
    public IWorldConfig getWorldConfig() {
        return this.worldConfig;
    }

    @Override
    public IBiomeConfig getBiomeConfig(String biomeName) {
        return this.biomeConfigs.get(biomeName);
    }

    @Override
    public IMapImageProvider getMapImageSource() {
        if (this.mapImage != null) return this.mapImage;
        if (this.mapImageOffset != -1) {
            try {
                this.channel.position(this.mapImageOffset);
                int length = stream.readInt();
                byte[] data = new byte[length];
                int numRead = stream.read(data);
                if (numRead != length) {
                    throw new IOException("failed to read whole image");
                }

                this.mapImage = new ByteArrayMapImageProvider(data);
                return this.mapImage;
            } catch (IOException e) {
                OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.MAIN, "Unable to load map image: " + e);
            }
        }
        return null;
    }

    @Override
    public ArrayList<IBiomeConfig> getAllBiomeConfigs() {
        return new ArrayList<>(this.biomeConfigs.values());
    }

    @Override
    public ArrayList<String> getAllBiomeNames() {
        return new ArrayList<>(this.biomeConfigs.keySet());
    }

    @Override
    public ICustomObject getCustomObject(String name) {
        Long objectOffset = this.objectOffsets.get(name);
        if (objectOffset == null) return null;
        try {
            channel.position(objectOffset);
            byte type = stream.readByte();
            int len = stream.readInt();
            byte[] data = new byte[len];
            int got = stream.read(data);
            if (got != len) {
                OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.CUSTOM_OBJECTS, "Truncated packed biome object " + name + "!");
                return null;
            }
            switch (type) {
                case 2:  // BO2
                    BO2 config2 = BO2.readFromStream(new DataInputStream(new ByteArrayInputStream(data)), OTG.getEngine().getLogger(), OTG.getEngine().getPresetLoader().getMaterialReader(this.shortName), materialPalette, metadataPalette);
                    config2.overrideName(name);
                    return config2;
                case 3:  // BO3
                    BO3Config config3 = BO3Config.readFromStream(new DataInputStream(new ByteArrayInputStream(data)), this.shortName, OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(this.shortName), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker(), materialPalette, metadataPalette);
                    config3.overrideName(name);
                    return new BO3(name, null, config3);
                case 4:  // BO4
                    BO4Config config4 = new BO4Config(null, false, this.shortName, OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(this.shortName), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());
                    config4.readFromStream(true, new DataInputStream(new ByteArrayInputStream(data)), OTG.getEngine().getLogger(), OTG.getEngine().getPresetLoader().getMaterialReader(this.shortName), materialPalette, metadataPalette, true);
                    config4.overrideName(name);
                    return new BO4(name, null, config4);
            }
        } catch (Exception e) {
            OTG.getEngine().getLogger().log(LogLevel.ERROR, LogCategory.CUSTOM_OBJECTS, "Failed to load packed custom object " + name + ":");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public int getMajorVersion() {
        return this.majorVersion;
    }

    @Override
    public boolean isPacked() {
        return true;
    }
}
