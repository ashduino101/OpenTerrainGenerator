package com.pg85.otg.presets;

import com.pg85.otg.OTG;
import com.pg85.otg.config.ConfigFunction;
import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.io.FileSettingsPacker;
import com.pg85.otg.config.io.FileSettingsReader;
import com.pg85.otg.config.io.NameTable;
import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.constants.SettingsEnums;
import com.pg85.otg.customobject.CustomObject;
import com.pg85.otg.customobject.bo3.BO3;
import com.pg85.otg.customobject.bo3.BO3Config;
import com.pg85.otg.customobject.bo3.bo3function.BO3BlockFunction;
import com.pg85.otg.customobject.bo3.bo3function.BO3BranchFunction;
import com.pg85.otg.customobject.bo4.BO4;
import com.pg85.otg.customobject.bo4.BO4Config;
import com.pg85.otg.customobject.bo4.BO4Data;
import com.pg85.otg.customobject.bo4.BO4NBTPacker;
import com.pg85.otg.customobject.bofunctions.BlockFunction;
import com.pg85.otg.customobject.bofunctions.BranchFunction;
import com.pg85.otg.customobject.config.CustomObjectConfigFile;
import com.pg85.otg.customobject.config.io.FileSettingsReaderBO4;
import com.pg85.otg.customobject.config.io.SettingsReaderBO4;
import com.pg85.otg.customobject.creator.ObjectCreator;
import com.pg85.otg.customobject.creator.ObjectType;
import com.pg85.otg.customobject.resource.CustomStructureResource;
import com.pg85.otg.customobject.structures.Branch;
import com.pg85.otg.customobject.structures.StructuredCustomObject;
import com.pg85.otg.customobject.structures.bo4.BO4CustomStructure;
import com.pg85.otg.customobject.structures.bo4.BO4CustomStructureCoordinate;
import com.pg85.otg.customobject.util.BoundingBox;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.interfaces.*;
import com.pg85.otg.util.bo3.Rotation;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import com.pg85.otg.util.nbt.NamedBinaryTag;
import org.tukaani.xz.LZMA2Options;
import org.tukaani.xz.XZOutputStream;

/**
 * A packer for OTG presets.
 */
public class PresetPacker
{
    public PresetPacker() {
//        LZMA2Options options = new LZMA2Options();
    }

    private static final String magic = "OTG\n";
    private static final int version = 1;
    public static void packToFile(Preset preset, FileOutputStream file, ILogger logger) throws IOException
    {
        DataOutputStream stream = new DataOutputStream(file);
        FileChannel channel = file.getChannel();

        stream.writeBytes(magic);
        stream.writeInt(version);
        stream.writeLong(0);  // Will be filled in later

        NameTable nameTable = new NameTable();

        LZMA2Options options = new LZMA2Options();
        options.setPreset(6);

        Path presetDir = preset.getPresetFolder();

        // Write world config
        long worldConfigOffset = channel.position();  // Should always be after the header, but we'll check just in case
        File worldConfigFile = new File(presetDir.toString(), Constants.WORLD_CONFIG_FILE);
        SettingsMap worldConfigSettings = FileSettingsReader.read(preset.getFolderName(), worldConfigFile, logger);
        FileSettingsPacker.packToStream(worldConfigSettings, stream, logger, nameTable);
        stream.flush();

        // Write map image if necessary
        long mapOffset = -1;
        if (preset.getWorldConfig().getBiomeMode() == SettingsEnums.BiomeMode.FromImage) {
            File imageFile = new File(presetDir.toString(), preset.getWorldConfig().getImageFile());
            BufferedInputStream imageIn = new BufferedInputStream(Files.newInputStream(imageFile.toPath()));
            long imageSize = imageFile.length();
            OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.MAIN, String.valueOf(imageSize));

            byte[] imageData = new byte[(int) imageSize];
            mapOffset = channel.position();
            int bytesRead = imageIn.read(imageData);
            OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.MAIN, String.valueOf(bytesRead));

            stream.write(imageData, 0, bytesRead);
            stream.flush();
        }

        // Write biome configs
        HashMap<String, Long> biomeConfigOffsets = new HashMap<>();
        for (IBiomeConfig biomeConfig : preset.getAllBiomeConfigs())
        {
            ByteArrayOutputStream preCompress = new ByteArrayOutputStream();
            DataOutput preCompOut = new DataOutputStream(preCompress);
            SettingsMap settings = ((BiomeConfig)biomeConfig).getSettingsAsMap();
            biomeConfigOffsets.put(biomeConfig.getName(), channel.position());
            FileSettingsPacker.packToStream(settings, preCompOut, logger, nameTable);
            XZOutputStream out = new XZOutputStream(stream, options);
            out.write(preCompress.toByteArray(), 0, preCompress.size());
            out.finish();
            stream.flush();
        }

        // Write biome objects
        HashMap<String, Long> biomeObjectOffsets = new HashMap<>();
        for (IBiomeConfig biomeConfig : preset.getAllBiomeConfigs())
        {
            for (ConfigFunction<IBiomeConfig> res : ((BiomeConfig)biomeConfig).getResourceQueue())
            {
                if (res instanceof CustomStructureResource)
                {
                    for (IStructuredCustomObject structure : ((CustomStructureResource)res).getObjects(preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getFolderName()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker()))
                    {
                        if (structure != null)  // Structure was in resource list but file could not be found.
                        {
                            if (structure instanceof BO4)
                            {
                                BO4NBTPacker bnp = new BO4NBTPacker((BO4) structure);

                                ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
                                DataOutputStream dataOut = new DataOutputStream(byteArrayOut);

                                biomeObjectOffsets.put(structure.getName(), channel.position());

                                BO4Data.generateBO4DataToStream(((BO4)structure).getConfig(), dataOut, preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getFolderName()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker(), true);

                                stream.writeInt(byteArrayOut.size());
                                stream.write(byteArrayOut.toByteArray(), 0, byteArrayOut.size());

                                OTG.getEngine().getCustomObjectManager().getGlobalObjects().unloadCustomObjectFiles();
                            }
                        }
                    }
                }
            }
        }
        ArrayList<String> boNames = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getAllBONamesForPreset(preset.getFolderName(), OTG.getEngine().getLogger(), OTG.getEngine().getOTGRootFolder());

        for (String boName : boNames)
        {
            CustomObject bo = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getObjectByName(boName, preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getFolderName()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());
            if (bo instanceof BO4)
            {
                stream.writeByte(4);  // BO4

                ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
                DataOutputStream dataOut = new DataOutputStream(byteArrayOut);

                biomeObjectOffsets.put(bo.getName(), channel.position());

                BO4Data.generateBO4DataToStream(((BO4) bo).getConfig(), dataOut, preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), logger, OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getFolderName()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker(), true);

                stream.writeInt(byteArrayOut.size());
                stream.write(byteArrayOut.toByteArray(), 0, byteArrayOut.size());

                OTG.getEngine().getCustomObjectManager().getGlobalObjects().unloadCustomObjectFiles();
            }
            else if (bo instanceof BO3)
            {
                stream.writeByte(3);  // BO3

                ByteArrayOutputStream preCompress = new ByteArrayOutputStream();
                DataOutput preCompOut = new DataOutputStream(preCompress);
                biomeObjectOffsets.put(bo.getName(), channel.position());
                XZOutputStream out = new XZOutputStream(stream, options);  // We LZMA-compress serialized BO3s to save space -- we don't do this with BO4Datas because they are already zlib-compressed
                FileSettingsPacker.packToStream(FileSettingsReader.read(bo.getName(), ((BO3) bo).getConfig().getFile(), logger), preCompOut, logger, nameTable);
                out.write(preCompress.toByteArray(), 0, preCompress.size());
                out.finish();
                stream.flush();
            } else {
                continue;  // no BO2 support
            }
            stream.flush();
        }


        // Here comes the metadata -- fill in the offset in the header
        long metadataOffset = channel.position();
        channel.position(8);  // sizeof(magic) + sizeof(version)
        stream.writeLong(metadataOffset);
        channel.position(metadataOffset);

        // Name table
        nameTable.packToStream(stream);
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

        // Dependency offsets
    }
}
