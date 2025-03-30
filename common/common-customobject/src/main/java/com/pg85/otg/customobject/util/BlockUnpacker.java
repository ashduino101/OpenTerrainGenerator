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

    public List<BlockFunction<?>> unpackFromStream(DataInputStream stream, IBlockFunctionFactory nonRandomBlockFactory, IMaterialReader materialReader, ILogger logger, MaterialPalette materialPalette) throws IOException, InvalidConfigException {
        List<BlockFunction<?>> blocks = new ArrayList<>();

        // Nonrandom blocks
        boolean hasNonRandomBlocks = stream.readBoolean();
        if (hasNonRandomBlocks) {
            String[] metaDataNamesArr = new String[stream.readShort()];
            for (int i = 0; i < metaDataNamesArr.length; i++) {
                metaDataNamesArr[i] = StreamHelper.readStringFromStream(stream);
            }

            LocalMaterialData[] materialsArr = new LocalMaterialData[stream.readShort()];
            for (int i = 0; i < materialsArr.length; i++) {
                String materialName;
                if (materialPalette == null) {
                    materialName = StreamHelper.readStringFromStream(stream);
                } else {
                    materialName = materialPalette.getMaterial(stream.readUnsignedShort());
                }
                try {
                    materialsArr[i] = materialReader.readMaterial(materialName);
                } catch (InvalidConfigException e) {
                    if (logger.getLogCategoryEnabled(LogCategory.CUSTOM_OBJECTS)) {
                        logger.log(LogLevel.ERROR, LogCategory.CUSTOM_OBJECTS, "Could not read material \"" + materialName + "\".");
                    }
                }
            }

            short minX = stream.readShort();
            short maxX = stream.readShort();
            short minY = stream.readShort();
            short maxY = stream.readShort();
            short minZ = stream.readShort();
            short maxZ = stream.readShort();

            int sizeX = maxX - minX;
            int sizeY = maxY - minY;

            int numBlocks = stream.readInt();
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

            int numNbt = stream.readInt();
            for (int i = 0; i < numNbt; i++) {
                short x = stream.readShort();
                short y = stream.readShort();
                short z = stream.readShort();
                short idx = stream.readShort();
                Optional<BlockFunction<?>> block = blocks.stream().filter(b -> b.x == x && b.y == y && b.z == z).findFirst();
                block.ifPresent(blockFunction -> blockFunction.nbtName = metaDataNamesArr[idx]);
            }
        }

        // Random blocks
        boolean hasRandomBlocks = stream.readBoolean();
        if (hasRandomBlocks) {
            int materialPaletteSize = stream.readInt();
            LocalMaterialData[] materials = new LocalMaterialData[materialPaletteSize];
            for (int i = 0; i < materialPaletteSize; i++) {
                if (materialPalette == null) {
                    materials[i] = materialReader.readMaterial(StreamHelper.readStringFromStream(stream));
                } else {
                    materials[i] = materialReader.readMaterial(materialPalette.getMaterial(stream.readUnsignedShort()));
                }
            }

            int metaDataPaletteSize = stream.readInt();
            String[] metaDataPalette = new String[metaDataPaletteSize];
            for (int i = 0; i < metaDataPaletteSize; i++) {
                metaDataPalette[i] = StreamHelper.readStringFromStream(stream);
            }

            int numRandomBlocks = stream.readInt();
            byte randomBlockType = stream.readByte();  // 3 = BO3, 4 = BO4
            if (randomBlockType == 3) {
                for (int i = 0; i < numRandomBlocks; i++) {
                    BO3RandomBlockFunction rbf = new BO3RandomBlockFunction();
                    rbf.x = stream.readByte();
                    rbf.y = stream.readShort();
                    rbf.z = stream.readByte();

                    rbf.blockCount = stream.readByte();
                    rbf.blocks = new LocalMaterialData[rbf.blockCount];
                    rbf.blockChances = new byte[rbf.blockCount];
                    rbf.metaDataNames = new String[rbf.blockCount];
                    for (int j = 0; j < rbf.blockCount; j++) {
                        rbf.blockChances[j] = stream.readByte();
                        short blockIdx = stream.readShort();
                        rbf.blocks[j] = blockIdx == 0 ? null : materials[blockIdx - 1];
                        short metaDataIdx = stream.readShort();
                        rbf.metaDataNames[j] = metaDataIdx == 0 ? null : metaDataPalette[metaDataIdx - 1];
                    }
                }
            } else if (randomBlockType == 4) {
                for (int i = 0; i < numRandomBlocks; i++) {
                    BO4RandomBlockFunction rbf = new BO4RandomBlockFunction();
                    rbf.x = stream.readByte();
                    rbf.y = stream.readShort();
                    rbf.z = stream.readByte();

                    rbf.blockCount = stream.readByte();
                    rbf.blocks = new LocalMaterialData[rbf.blockCount];
                    rbf.blockChances = new byte[rbf.blockCount];
                    rbf.metaDataNames = new String[rbf.blockCount];
                    for (int j = 0; j < rbf.blockCount; j++) {
                        rbf.blockChances[j] = stream.readByte();
                        short blockIdx = stream.readShort();
                        rbf.blocks[j] = blockIdx == 0 ? null : materials[blockIdx - 1];
                        short metaDataIdx = stream.readShort();
                        rbf.metaDataNames[j] = metaDataIdx == 0 ? null : metaDataPalette[metaDataIdx - 1];
                    }
                }
            } else {
                throw new InvalidConfigException("expected a random block type of either 3 or 4 but got " + randomBlockType);
            }
        }

        return blocks;
    }
}
