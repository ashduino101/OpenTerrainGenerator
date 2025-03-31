package com.pg85.otg.customobject.util;

import com.pg85.otg.customobject.bo3.bo3function.BO3RandomBlockFunction;
import com.pg85.otg.customobject.bo4.bo4function.BO4RandomBlockFunction;
import com.pg85.otg.customobject.bofunctions.BlockFunction;
import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.interfaces.IMaterialReader;
import com.pg85.otg.util.helpers.StreamHelper;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.MaterialPalette;
import com.pg85.otg.util.nbt.NBTPalette;
import com.pg85.otg.util.nbt.NamedBinaryTag;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Unpacks blocks from longs in a similar way to vanilla region files
 */
public class BlockUnpacker {
    public BlockUnpacker() {

    }

    public List<BlockFunction<?>> unpackFromStream(DataInputStream stream, IBlockFunctionFactory nonRandomBlockFactory, IMaterialReader materialReader, ILogger logger, MaterialPalette materialPalette, NBTPalette nbtPalette) throws IOException, InvalidConfigException {
        List<BlockFunction<?>> blocks = new ArrayList<>();

        // Nonrandom blocks
        boolean hasNonRandomBlocks = stream.readBoolean();
        if (hasNonRandomBlocks) {
            String[] metaDataNamesArr = new String[StreamHelper.readVarIntFromStream(stream)];
            for (int i = 0; i < metaDataNamesArr.length; i++) {
                if (nbtPalette == null) {
                    metaDataNamesArr[i] = StreamHelper.readStringFromStream(stream);
                } else {
                    metaDataNamesArr[i] = nbtPalette.getNameFromIndex(StreamHelper.readVarIntFromStream(stream));
                }
            }

            LocalMaterialData[] materialsArr = new LocalMaterialData[StreamHelper.readVarIntFromStream(stream)];
            for (int i = 0; i < materialsArr.length; i++) {
                String materialName;
                if (materialPalette == null) {
                    materialName = StreamHelper.readStringFromStream(stream);
                } else {
                    materialName = materialPalette.getMaterial(StreamHelper.readVarIntFromStream(stream));
                }
                try {
                    materialsArr[i] = materialReader.readMaterial(materialName);
                } catch (InvalidConfigException e) {
                    if (logger.getLogCategoryEnabled(LogCategory.CUSTOM_OBJECTS)) {
                        logger.log(LogLevel.ERROR, LogCategory.CUSTOM_OBJECTS, "Could not read material \"" + materialName + "\".");
                    }
                }
            }

            int minX = StreamHelper.readVarIntFromStream(stream);
            int maxX = StreamHelper.readVarIntFromStream(stream);
            int minY = StreamHelper.readVarIntFromStream(stream);
            int maxY = StreamHelper.readVarIntFromStream(stream);
            int minZ = StreamHelper.readVarIntFromStream(stream);
            int maxZ = StreamHelper.readVarIntFromStream(stream);

            int sizeX = maxX - minX;
            int sizeY = maxY - minY;

            int numBlocks = StreamHelper.readVarIntFromStream(stream);
            byte bitsPerBlock = stream.readByte();
            long mask = (1L << ((long) bitsPerBlock)) - 1;

            byte blocksPerLong = (byte) (64 / bitsPerBlock);
            int numLongs = (int) Math.ceil((double) numBlocks / blocksPerLong);

            for (int i = 0; i < numLongs; i++) {
                long value = stream.readLong();
                for (int j = 0; j < blocksPerLong; j++) {
                    int blockIndex = i * blocksPerLong + j;
                    if (blockIndex >= numBlocks) break;
                    byte bitPos = (byte) (j * bitsPerBlock);
                    int material = (int) ((value & (mask << bitPos)) >> bitPos) - 1;

                    // i hate java i hate java i hate java
                    if (material < -1) {
                        material += (1 << bitsPerBlock);
                    }

                    if (material != -1) {
                        int idx = blockIndex;
                        final int z = idx / (sizeX * sizeY);
                        idx -= (z * sizeX * sizeY);
                        final int y = idx / sizeX;
                        final int x = idx % sizeX;

                        BlockFunction<?> func = nonRandomBlockFactory.createFunction();
                        func.x = minX + x;
                        func.y = (short) (minY + y);
                        func.z = minZ + z;
                        func.material = materialsArr[material];
                        blocks.add(func);
                    }
                }
            }

            int numNbt = StreamHelper.readVarIntFromStream(stream);
            for (int i = 0; i < numNbt; i++) {
                int x = StreamHelper.readVarIntFromStream(stream);
                int y = StreamHelper.readVarIntFromStream(stream);
                int z = StreamHelper.readVarIntFromStream(stream);
                int idx = StreamHelper.readVarIntFromStream(stream);
                Optional<BlockFunction<?>> block = blocks.stream().filter(b -> b.x == x && b.y == y && b.z == z).findFirst();
                block.ifPresent(blockFunction -> {
                    blockFunction.nbtName = metaDataNamesArr[idx];
                    if (nbtPalette != null) {
                        // Load the packed NBT
                        blockFunction.nbt = nbtPalette.getNBTFromName(blockFunction.nbtName);
                    }
                });
            }
        }

        // Random blocks
        boolean hasRandomBlocks = stream.readBoolean();
        if (hasRandomBlocks) {
            int materialPaletteSize = StreamHelper.readVarIntFromStream(stream);
            LocalMaterialData[] materials = new LocalMaterialData[materialPaletteSize];
            for (int i = 0; i < materialPaletteSize; i++) {
                if (materialPalette == null) {
                    materials[i] = materialReader.readMaterial(StreamHelper.readStringFromStream(stream));
                } else {
                    materials[i] = materialReader.readMaterial(materialPalette.getMaterial(StreamHelper.readVarIntFromStream(stream)));
                }
            }

            int metaDataPaletteSize = StreamHelper.readVarIntFromStream(stream);
            String[] metaDataPalette = new String[metaDataPaletteSize];
            for (int i = 0; i < metaDataPaletteSize; i++) {
                if (nbtPalette == null) {
                    metaDataPalette[i] = StreamHelper.readStringFromStream(stream);
                } else {
                    metaDataPalette[i] = nbtPalette.getNameFromIndex(StreamHelper.readVarIntFromStream(stream));
                }
            }

            int numRandomBlocks = StreamHelper.readVarIntFromStream(stream);
            byte randomBlockType = stream.readByte();  // 3 = BO3, 4 = BO4
            if (randomBlockType == 3) {
                for (int i = 0; i < numRandomBlocks; i++) {
                    BO3RandomBlockFunction rbf = new BO3RandomBlockFunction();
                    rbf.x = stream.readByte();
                    rbf.y = (short) StreamHelper.readVarIntFromStream(stream);
                    rbf.z = stream.readByte();

                    rbf.blockCount = stream.readByte();
                    rbf.blocks = new LocalMaterialData[rbf.blockCount];
                    rbf.blockChances = new byte[rbf.blockCount];
                    rbf.metaDataNames = new String[rbf.blockCount];
                    rbf.metaDataTags = new NamedBinaryTag[rbf.blockCount];
                    for (int j = 0; j < rbf.blockCount; j++) {
                        rbf.blockChances[j] = stream.readByte();
                        int blockIdx = StreamHelper.readVarIntFromStream(stream);
                        rbf.blocks[j] = blockIdx == 0 ? null : materials[blockIdx - 1];
                        int metaDataIdx = StreamHelper.readVarIntFromStream(stream);
                        rbf.metaDataNames[j] = metaDataIdx == 0 ? null : metaDataPalette[metaDataIdx - 1];
                        if (rbf.metaDataNames[j] != null && nbtPalette != null) {
                            rbf.metaDataTags[j] = nbtPalette.getNBTFromName(rbf.metaDataNames[j]);
                        }
                    }
                }
            } else if (randomBlockType == 4) {
                for (int i = 0; i < numRandomBlocks; i++) {
                    BO4RandomBlockFunction rbf = new BO4RandomBlockFunction();
                    rbf.x = stream.readByte();
                    rbf.y = (short) StreamHelper.readVarIntFromStream(stream);
                    rbf.z = stream.readByte();

                    rbf.blockCount = stream.readByte();
                    rbf.blocks = new LocalMaterialData[rbf.blockCount];
                    rbf.blockChances = new byte[rbf.blockCount];
                    rbf.metaDataNames = new String[rbf.blockCount];
                    rbf.metaDataTags = new NamedBinaryTag[rbf.blockCount];
                    for (int j = 0; j < rbf.blockCount; j++) {
                        rbf.blockChances[j] = stream.readByte();
                        int blockIdx = StreamHelper.readVarIntFromStream(stream);
                        rbf.blocks[j] = blockIdx == 0 ? null : materials[blockIdx - 1];
                        int metaDataIdx = StreamHelper.readVarIntFromStream(stream);
                        rbf.metaDataNames[j] = metaDataIdx == 0 ? null : metaDataPalette[metaDataIdx - 1];
                        if (rbf.metaDataNames[j] != null && nbtPalette != null) {
                            rbf.metaDataTags[j] = nbtPalette.getNBTFromName(rbf.metaDataNames[j]);
                        }
                    }
                }
            } else {
                throw new InvalidConfigException("expected a random block type of either 3 or 4 but got " + randomBlockType);
            }
        }

        return blocks;
    }
}
