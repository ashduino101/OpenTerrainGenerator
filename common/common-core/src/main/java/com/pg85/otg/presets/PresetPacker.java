package com.pg85.otg.presets;

import com.pg85.otg.OTG;
import com.pg85.otg.config.biome.BiomeConfig;
import com.pg85.otg.config.io.PackedFileSettings;
import com.pg85.otg.config.io.FileSettingsReader;
import com.pg85.otg.config.io.NameTable;
import com.pg85.otg.config.io.SettingsMap;
import com.pg85.otg.constants.Constants;
import com.pg85.otg.constants.SettingsEnums;
import com.pg85.otg.customobject.CustomObject;
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
import com.pg85.otg.util.nbt.NamedBinaryTag;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

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
    public static void packToFile(Preset preset, FileOutputStream file, ILogger logger) throws IOException {
        DataOutputStream stream = new DataOutputStream(file);
        FileChannel channel = file.getChannel();

        stream.writeBytes(magic);
        stream.writeInt(version);
        stream.writeLong(0);  // Will be filled in later

        NameTable nameTable = new NameTable();

        Path presetDir = preset.getPresetFolder();

        // Write world config
        long worldConfigOffset = channel.position();  // Should always be after the header, but we'll check just in case
        File worldConfigFile = new File(presetDir.toString(), Constants.WORLD_CONFIG_FILE);
        SettingsMap worldConfigSettings = FileSettingsReader.read(preset.getFolderName(), worldConfigFile, logger);
        PackedFileSettings.packToStream(worldConfigSettings, stream, logger, nameTable);
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

            // We don't need to compress it since it's already zlib-compressed per PNG spec
            stream.write(imageData, 0, bytesRead);
            stream.flush();
        }

        // Write biome configs
        HashMap<String, Long> biomeConfigOffsets = new HashMap<>();
        for (IBiomeConfig biomeConfig : preset.getAllBiomeConfigs())
        {
            SettingsMap settings = ((BiomeConfig)biomeConfig).getSettingsAsMap();
            biomeConfigOffsets.put(biomeConfig.getName(), channel.position());
            PackedFileSettings.packToStream(settings, stream, logger, nameTable);
            stream.flush();
        }

        // Write biome objects
        HashMap<String, Long> biomeObjectOffsets = new HashMap<>();

        ArrayList<String> boNames = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getAllBONamesForPreset(preset.getFolderName(), OTG.getEngine().getLogger(), OTG.getEngine().getOTGRootFolder());

        Map<String, NamedBinaryTag> nbtFiles = new HashMap<>();

        for (String boName : boNames) {
            CustomObject object = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getObjectByName(boName, preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getFolderName()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());
            {
                if (object != null)  // Structure was in resource list but file could not be found.
                {
                    if (object instanceof BO4) {
                        stream.writeByte(4);
                        packBO4((BO4) object, channel, stream, preset, biomeObjectOffsets, nbtFiles);
                    } else if (object instanceof BO3) {
                        stream.writeByte(3);
                        packBO3((BO3) object, channel, stream, preset, biomeObjectOffsets, nbtFiles);
                    }
                }
            }
        }

        OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.MAIN, String.format("Writing %d NBT files", nbtFiles.size()));

        Map<String, byte[]> nbt = new HashMap<>();
        Set<byte[]> containedHashes = new HashSet<>();
        for (Map.Entry<String, NamedBinaryTag> tag : nbtFiles.entrySet()) {
            NamedBinaryTag value = tag.getValue();
            if (value == null) continue;
            ByteArrayOutputStream nbtStream = new ByteArrayOutputStream();
            value.writeTo(nbtStream, false);
            byte[] arr = nbtStream.toByteArray();
            try {
                MessageDigest md = MessageDigest.getInstance("MD5");
                byte[] hash = md.digest(arr);
                if (!containedHashes.contains(hash)) {
                    containedHashes.add(hash);
                    nbt.put(tag.getKey(), arr);
                }
            } catch (NoSuchAlgorithmException e) {
                // unreachable (hopefully)
            }
        }

        long nbtOffset = channel.position();

        stream.writeInt(nbt.size());
        for (Map.Entry<String, byte[]> tag : nbt.entrySet()) {
            stream.writeUTF(tag.getKey());
            byte[] val = tag.getValue();
            stream.writeInt(val.length);
            stream.write(val);
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

        // Resource offsets
        stream.writeLong(nbtOffset);
    }

//    private static void scanEntries(File dir, HashMap<String, IStructuredCustomObject> objects, BO3Loader bo3Loader, BO4Loader bo4Loader) {
//        for (File f : dir.listFiles()) {
//            if (f.isDirectory()) {
//                scanEntries(f);
//            } else {
//                String name = f.getName();
//
//                if ()
//                objects.put(f.getName(), )
//            }
//        }
//    }

    private static void packBO4(BO4 object, FileChannel channel, DataOutputStream stream, Preset preset, HashMap<String, Long> offsets, Map<String, NamedBinaryTag> nbtFiles) throws IOException {
        OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.MAIN, object.getName());
//        BO4NBTPacker bnp = new BO4NBTPacker(object);

        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteArrayOut);

        long offset = channel.position();

        BO4Data.generateBO4DataToStream((object).getConfig(), dataOut, preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getFolderName()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker(), false);

        offsets.put(object.getName(), offset);

        stream.writeInt(byteArrayOut.size());
        stream.write(byteArrayOut.toByteArray(), 0, byteArrayOut.size());
        stream.flush();

        BlockFunction<?>[] funcs = object.getConfig().getBlockFunctions(preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getFolderName()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());
        for (BlockFunction<?> func : funcs) {
            nbtFiles.putIfAbsent(func.nbtName, func.nbt);
        }

        for (Branch branch : object.getBranches()) {
            if (branch instanceof BO4BranchFunction) {
                List<String> branchNames = ((BO4BranchFunction)branch).getBranchObjectNames();
                for (String name : branchNames) {
                    CustomObject bo = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getObjectByName(name, preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getFolderName()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());
                    if (bo == null) {
                        OTG.getEngine().getLogger().log(LogLevel.WARN, LogCategory.MAIN, String.format("Skipping non-existent branch '%s'", name));
                        continue;
                    }
                    if (!offsets.containsKey(bo.getName())) {
                        if (bo instanceof BO4) {
                            packBO4((BO4)bo, channel, stream, preset, offsets, nbtFiles);
                        } else if (bo instanceof BO3) {
                            packBO3((BO3)bo, channel, stream, preset, offsets, nbtFiles);
                        }
                    }
                }
            }
        }

//        OTG.getEngine().getCustomObjectManager().getGlobalObjects().unloadCustomObjectFiles();
    }

    public static void packBO3(BO3 object, FileChannel channel, DataOutputStream stream, Preset preset, HashMap<String, Long> offsets, Map<String, NamedBinaryTag> nbtFiles) throws IOException {
        OTG.getEngine().getLogger().log(LogLevel.INFO, LogCategory.MAIN, object.getName());

        ByteArrayOutputStream byteArrayOut = new ByteArrayOutputStream();
        DataOutputStream dataOut = new DataOutputStream(byteArrayOut);

        long offset = channel.position();

        object.getConfig().writeToStream(dataOut, preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getFolderName()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());

        offsets.put(object.getName(), offset);

        stream.writeInt(byteArrayOut.size());
        stream.write(byteArrayOut.toByteArray(), 0, byteArrayOut.size());
        stream.flush();

        BlockFunction<?>[] funcs = object.getConfig().getBlocks(0);
        for (BlockFunction<?> func : funcs) {
            nbtFiles.putIfAbsent(func.nbtName, func.nbt);
        }

        for (Branch branch : object.getBranches(Rotation.NORTH)) {
            if (branch instanceof BO3BranchFunction) {
                List<String> branchNames = ((BO3BranchFunction)branch).getBranchObjectNames();
                for (String name : branchNames) {
                    CustomObject bo = OTG.getEngine().getCustomObjectManager().getGlobalObjects().getObjectByName(name, preset.getFolderName(), OTG.getEngine().getOTGRootFolder(), OTG.getEngine().getLogger(), OTG.getEngine().getCustomObjectManager(), OTG.getEngine().getPresetLoader().getMaterialReader(preset.getFolderName()), OTG.getEngine().getCustomObjectResourcesManager(), OTG.getEngine().getModLoadedChecker());
                    if (bo == null) {
                        OTG.getEngine().getLogger().log(LogLevel.WARN, LogCategory.MAIN, String.format("Skipping non-existent branch '%s'", name));
                        continue;
                    }
                    if (!offsets.containsKey(bo.getName())) {
                        if (bo instanceof BO4) {
                            packBO4((BO4)bo, channel, stream, preset, offsets, nbtFiles);
                        } else if (bo instanceof BO3) {
                            packBO3((BO3)bo, channel, stream, preset, offsets, nbtFiles);
                        }
                    }
                }
            }
        }
    }
}
