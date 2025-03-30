package com.pg85.otg.presets;

import com.pg85.otg.OTG;
import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.io.PackedFileSettings;
import com.pg85.otg.config.io.FileSettingsReader;
import com.pg85.otg.util.StringTable;
import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.constants.SettingsEnums;
import com.pg85.otg.customobject.CustomObject;
import com.pg85.otg.customobject.bo2.BO2;
import com.pg85.otg.customobject.bo3.BO3;
import com.pg85.otg.customobject.bo3.bo3function.BO3BranchFunction;
import com.pg85.otg.customobject.bo4.BO4;
import com.pg85.otg.customobject.bo4.BO4Data;
import com.pg85.otg.customobject.bo4.bo4function.BO4BranchFunction;
import com.pg85.otg.customobject.bofunctions.BlockFunction;
import com.pg85.otg.customobject.structures.Branch;
import com.pg85.otg.interfaces.*;
import com.pg85.otg.util.bo3.Rotation;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import com.pg85.otg.util.materials.MaterialPalette;
import com.pg85.otg.util.nbt.NBTPalette;
import com.pg85.otg.util.nbt.NamedBinaryTag;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * A packer for OTG presets.
 */
public class PresetPacker
{
    private int processedBiomes = 0;
    private int totalBiomes = 0;
    private int processedObjects = 0;
    private int totalObjects = 0;
    private PackingStage stage = PackingStage.Starting;
    private final List<String> currentObjectPath = new ArrayList<>();

    public PresetPacker() {

    }

    static final String magic = "OTG\n";
    private static final int version = PackedPreset.version;
    public void packToFile(PresetFolder preset, FileOutputStream file, ILogger logger) throws IOException {
        DataOutputStream stream = new DataOutputStream(file);
        FileChannel channel = file.getChannel();

        stream.writeBytes(magic);
        stream.writeInt(version);
        stream.writeLong(0);  // Will be filled in later

        StringTable nameTable = new StringTable();
        MaterialPalette materialPalette = new MaterialPalette();
        NBTPalette nbtPalette = new NBTPalette();

        Path presetDir = preset.getPresetFolder();

        // Write world config
        this.stage = PackingStage.WorldConfig;
        long worldConfigOffset = channel.position();  // Should always be after the header, but we'll check just in case
        File worldConfigFile = new File(presetDir.toString(), Constants.WORLD_CONFIG_FILE);
        SettingsMap worldConfigSettings = FileSettingsReader.read(preset.getId(), worldConfigFile, logger);
        PackedFileSettings.packToStream(worldConfigSettings, stream, logger, nameTable);
        stream.flush();

        // Write map image if necessary
        long mapOffset = -1;
        if (preset.getWorldConfig().getBiomeMode() == SettingsEnums.BiomeMode.FromImage) {
            this.stage = PackingStage.MapImage;
            File imageFile = new File(presetDir.toString(), preset.getWorldConfig().getImageFile());
            BufferedInputStream imageIn = new BufferedInputStream(Files.newInputStream(imageFile.toPath()));
            long imageSize = imageFile.length();

            byte[] imageData = new byte[(int) imageSize];
            mapOffset = channel.position();
            int bytesRead = imageIn.read(imageData);

            // We don't need to compress it since it's already zlib-compressed per PNG spec
            stream.writeInt(bytesRead);
            stream.write(imageData, 0, bytesRead);
            stream.flush();
        }

        // Write biome configs
        this.stage = PackingStage.BiomeConfig;
        List<IBiomeConfig> biomeConfigs = preset.getAllBiomeConfigs();
        this.totalBiomes = biomeConfigs.size();
        this.processedBiomes = 0;
        HashMap<String, Long> biomeConfigOffsets = new HashMap<>();
        for (IBiomeConfig biomeConfig : biomeConfigs)
        {
            SettingsMap settings = ((BiomeConfig)biomeConfig).getSettingsAsMap();
            biomeConfigOffsets.put(biomeConfig.getName(), channel.position());
            PackedFileSettings.packToStream(settings, stream, logger, nameTable);
            stream.flush();
            this.processedBiomes++;
        }

        // Write biome objects
        this.stage = PackingStage.CustomObjects;
        HashMap<String, Long> biomeObjectOffsets = new HashMap<>();

        ArrayList<String> boNames = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getAllBONamesForPreset(preset.getId(), OTG.getEngine().getLogger(), OTG.getEngine().getOTGRootFolder());
        this.totalObjects = boNames.size();

        for (String boName : boNames) {
            CustomObject object = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getObjectByName(boName, preset.getId(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getId()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());
            {
                if (object != null)  // Structure was in resource list but file could not be found.
                {
                    if (object instanceof BO4) {
                        packBO4((BO4) object, channel, stream, preset, biomeObjectOffsets, materialPalette, nbtPalette);
                    } else if (object instanceof BO3) {
                        packBO3((BO3) object, channel, stream, preset, biomeObjectOffsets, materialPalette, nbtPalette);
                    } else if (object instanceof BO2) {
                        packBO2((BO2) object, channel, stream, biomeObjectOffsets, materialPalette, nbtPalette);
                    }
                }
            }
            this.processedObjects++;
        }

        this.stage = PackingStage.Resources;
        long nbtOffset = channel.position();
        nbtPalette.packToStream(stream);

        // Here comes the metadata -- fill in the offset in the header
        this.stage = PackingStage.MetaData;
        long metadataOffset = channel.position();
        channel.position(8);  // sizeof(magic) + sizeof(version)
        stream.writeLong(metadataOffset);
        channel.position(metadataOffset);

        // String table
        nameTable.packToStream(stream);
        stream.flush();

        // Material palette
        materialPalette.writeToStream(stream);
        stream.flush();

        // World config offset
        stream.writeLong(worldConfigOffset);

        // Map offset
        stream.writeLong(mapOffset);

        // TODO: no fallbacks system is implemented yet -- when it is, remember to implement it here and update the version

        // Biome config offsets
        stream.writeInt(biomeConfigOffsets.size());
        for (Map.Entry<String, Long> biome : biomeConfigOffsets.entrySet()) {
            stream.writeUTF(biome.getKey());
            stream.writeLong(biome.getValue());
        }

        // Object offsets
        stream.writeInt(biomeObjectOffsets.size());
        for (Map.Entry<String, Long> object : biomeObjectOffsets.entrySet()) {
            stream.writeUTF(object.getKey());
            stream.writeLong(object.getValue());
        }

        // Resource offsets
        stream.writeLong(nbtOffset);

        this.stage = PackingStage.Finished;
    }

    private void packBO4(BO4 object, FileChannel channel, DataOutputStream stream, PresetFolder preset, HashMap<String, Long> offsets, MaterialPalette materialPalette, NBTPalette metadataPalette) throws IOException {
        this.currentObjectPath.add(object.getName());

        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteArrayOut);

        long offset = channel.position();

        stream.writeByte(4);

        BO4Data.generateBO4DataToStream(object.getConfig(), true, dataOut, preset.getId(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getId()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker(), false, materialPalette, metadataPalette);

        offsets.put(object.getName(), offset);

        stream.writeInt(byteArrayOut.size());
        stream.write(byteArrayOut.toByteArray(), 0, byteArrayOut.size());
        stream.flush();

        for (Branch branch : object.getBranches()) {
            if (branch instanceof BO4BranchFunction) {
                List<String> branchNames = ((BO4BranchFunction)branch).getBranchObjectNames();
                this.totalObjects += branchNames.size();
                for (String name : branchNames) {
                    CustomObject bo = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getObjectByName(name, preset.getId(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getId()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());
                    if (bo == null) {
                        OTG.getEngine().getLogger().log(LogLevel.WARN, LogCategory.MAIN, String.format("Skipping non-existent branch '%s'", name));
                        this.processedObjects++;
                        continue;
                    }
                    if (!offsets.containsKey(bo.getName())) {
                        if (bo instanceof BO4) {
                            packBO4((BO4)bo, channel, stream, preset, offsets, materialPalette, metadataPalette);
                        } else if (bo instanceof BO3) {
                            packBO3((BO3)bo, channel, stream, preset, offsets, materialPalette, metadataPalette);
                        } else if (bo instanceof BO2) {
                            packBO2((BO2)bo, channel, stream, offsets, materialPalette, metadataPalette);
                        }
                    }
                    this.processedObjects++;
                }
            }
        }

        this.currentObjectPath.remove(this.currentObjectPath.size() - 1);
    }

    public void packBO3(BO3 object, FileChannel channel, DataOutputStream stream, PresetFolder preset, HashMap<String, Long> offsets, MaterialPalette materialPalette, NBTPalette metadataPalette) throws IOException {
        this.currentObjectPath.add(object.getName());

        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteArrayOut);

        long offset = channel.position();

        stream.writeByte(3);

        object.getConfig().writeToStream(dataOut, true, preset.getId(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getId()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker(), materialPalette, metadataPalette);

        offsets.put(object.getName(), offset);

        stream.writeInt(byteArrayOut.size());
        stream.write(byteArrayOut.toByteArray(), 0, byteArrayOut.size());
        stream.flush();

        for (Branch branch : object.getBranches(Rotation.NORTH)) {
            if (branch instanceof BO3BranchFunction) {
                List<String> branchNames = ((BO3BranchFunction)branch).getBranchObjectNames();
                this.totalObjects += branchNames.size();
                for (String name : branchNames) {
                    CustomObject bo = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getObjectByName(name, preset.getId(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getId()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());
                    if (bo == null) {
                        OTG.getEngine().getLogger().log(LogLevel.WARN, LogCategory.MAIN, String.format("Skipping non-existent branch '%s'", name));
                        this.processedObjects++;
                        continue;
                    }
                    if (!offsets.containsKey(bo.getName())) {
                        if (bo instanceof BO4) {
                            packBO4((BO4)bo, channel, stream, preset, offsets, materialPalette, metadataPalette);
                        } else if (bo instanceof BO3) {
                            packBO3((BO3)bo, channel, stream, preset, offsets, materialPalette, metadataPalette);
                        } else if (bo instanceof BO2) {
                            packBO2((BO2)bo, channel, stream, offsets, materialPalette, metadataPalette);
                        }
                    }
                    this.processedObjects++;
                }
            }
        }

        this.currentObjectPath.remove(this.currentObjectPath.size() - 1);
    }

    public void packBO2(BO2 object, FileChannel channel, DataOutputStream stream, HashMap<String, Long> offsets, MaterialPalette materialPalette, NBTPalette metadataPalette) throws IOException {
        this.currentObjectPath.add(object.getName());

        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteArrayOut);

        long offset = channel.position();

        stream.writeByte(2);

        object.writeToStream(dataOut, materialPalette, metadataPalette);

        offsets.put(object.getName(), offset);

        stream.writeInt(byteArrayOut.size());
        stream.write(byteArrayOut.toByteArray(), 0, byteArrayOut.size());
        stream.flush();

        this.currentObjectPath.remove(this.currentObjectPath.size() - 1);
    }

    enum PackingStage {
        Starting,
        WorldConfig,
        BiomeConfig,
        CustomObjects,
        MapImage,
        Resources,
        MetaData,
        Finished
    }

    public String getStatusString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%d/%d biomes packed, %d/%d objects packed, stage: %s", this.processedBiomes, this.totalBiomes, this.processedObjects, this.totalObjects, this.stage.toString()));
        if (this.stage == PackingStage.CustomObjects) {
            sb.append("\nCurrent branch depth: ").append(this.currentObjectPath.size()).append(", working on object: ").append(this.currentObjectPath.get(this.currentObjectPath.size() - 1));
        }
        return sb.toString();
    }
}
