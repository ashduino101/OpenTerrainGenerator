package com.pg85.otg.customobject.util;

import com.pg85.otg.util.Pair;
import com.pg85.otg.customobject.bo3.bo3function.BO3RandomBlockFunction;
import com.pg85.otg.customobject.bo4.bo4function.BO4RandomBlockFunction;
import com.pg85.otg.customobject.bofunctions.BlockFunction;
import com.pg85.otg.util.helpers.StreamHelper;
import com.pg85.otg.util.materials.LocalMaterialData;
import com.pg85.otg.util.materials.MaterialPalette;
import com.pg85.otg.util.nbt.NBTPalette;
import com.pg85.otg.util.nbt.NamedBinaryTag;

import java.io.DataOutput;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Packs blocks into longs in a similar way to vanilla region files
 */
public class BlockPacker {
    private final DataOutput stream;

    public BlockPacker(DataOutput output) {
        this.stream = output;
    }

    public void packToStream(List<BlockFunction<?>> blocks, MaterialPalette materialPalette, NBTPalette metadataPalette) throws IOException {
        List<BlockFunction<?>> nonRandomBlocks = blocks.stream().filter(b ->
                !(b instanceof BO3RandomBlockFunction || b instanceof BO4RandomBlockFunction)
        ).collect(Collectors.toList());

        // All blocks should have the same holder here
        String baseDir = blocks.isEmpty() ? null : blocks.get(0).getHolder().getFile().getParent();

        // Nonrandom blocks
        stream.writeBoolean(!nonRandomBlocks.isEmpty());  // hasNonRandomBlocks
        Map<String, String> relToAbsNBTPath = new HashMap<>();
        if (!nonRandomBlocks.isEmpty()) {
            ArrayList<String> materials = new ArrayList<>();
            ArrayList<String> metaDataNames = new ArrayList<>();
            HashMap<int[], Integer> blockNbt = new HashMap<>();
            for (BlockFunction<?> block : nonRandomBlocks) {
                // We have to convert these to strings here for some reason?
                // I think it's because ForgeMaterialData.equals() hasn't been updated to 1.16 yet
                if (block.material != null && !materials.contains(block.material.toString())) {
                    materials.add(block.material.toString());
                }
                if (block.nbtName != null && block.nbt != null) {
                    if (!metaDataNames.contains(block.nbtName)) {
                        metaDataNames.add(block.nbtName);
                    }
                    String path = baseDir + File.separator + block.nbtName;
                    Path p = Paths.get(path);
                    String abs = p.toAbsolutePath().normalize().toString();
                    relToAbsNBTPath.put(block.nbtName, abs);

                    if (metadataPalette != null) metadataPalette.getOrRegisterNBT(abs, block.nbt);
                    blockNbt.put(new int[]{block.x, block.y, block.z}, metaDataNames.indexOf(block.nbtName));
                }
            }

            String[] metaDataNamesArr = metaDataNames.toArray(new String[0]);

            // metadataNames should be empty if blocks is empty
            StreamHelper.writeVarIntToStream(stream, metaDataNamesArr.length);
            for (String s : metaDataNamesArr) {
                if (metadataPalette == null) {
                    StreamHelper.writeStringToStream(stream, s);
                } else {
                    StreamHelper.writeVarIntToStream(stream, metadataPalette.get(relToAbsNBTPath.get(s)));
                }
            }
            String[] materialsArr = materials.toArray(new String[0]);

            StreamHelper.writeVarIntToStream(stream, materialsArr.length);
            for (String localMaterialData : materialsArr) {
                if (materialPalette == null) {
                    StreamHelper.writeStringToStream(stream, localMaterialData);
                } else {
                    StreamHelper.writeVarIntToStream(stream, materialPalette.indexOrAddMaterial(localMaterialData));
                }
            }

            int bitsPerBlock = 32 - Integer.numberOfLeadingZeros(materialsArr.length);  // 32 - clz(n - 1)

            // TODO: This assumes that loading blocks in a different order won't matter, which may not be true?
            // Anything that spawns on top, entities/spawners etc, should be spawned last tho, so shouldn't be a problem?

            // Get bounds
            int minX = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int minY = Integer.MAX_VALUE;
            int maxY = Integer.MIN_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (BlockFunction<?> block : nonRandomBlocks) {
                if (block.x < minX) {
                    minX = block.x;
                }
                if (block.x > maxX) {
                    maxX = block.x;
                }
                if (block.y < minY) {
                    minY = block.y;
                }
                if (block.y > maxY) {
                    maxY = block.y;
                }
                if (block.z < minZ) {
                    minZ = block.z;
                }
                if (block.z > maxZ) {
                    maxZ = block.z;
                }
            }

            maxX += 1;
            maxY += 1;
            maxZ += 1;

            StreamHelper.writeVarIntToStream(stream, minX);
            StreamHelper.writeVarIntToStream(stream, maxX);
            StreamHelper.writeVarIntToStream(stream, minY);
            StreamHelper.writeVarIntToStream(stream, maxY);
            StreamHelper.writeVarIntToStream(stream, minZ);
            StreamHelper.writeVarIntToStream(stream, maxZ);

            // Convert the blocks to a 1-dimensional array
            int sizeX = maxX - minX;
            int sizeY = maxY - minY;
            int sizeZ = maxZ - minZ;
            List<BlockFunction<?>> blocksFlat = Arrays.asList(new BlockFunction<?>[sizeX * sizeY * sizeZ]);

            StreamHelper.writeVarIntToStream(stream, blocksFlat.size());
            stream.writeByte(bitsPerBlock);

            for (BlockFunction<?> block : nonRandomBlocks) {
                if (block instanceof BO3RandomBlockFunction || block instanceof BO4RandomBlockFunction) {
                    continue;
                }
                int normX = block.x - minX;
                int normY = block.y - minY;
                int normZ = block.z - minZ;
                int idx = (normZ * sizeX * sizeY) + (normY * sizeX) + normX;
                blocksFlat.set(idx, block);
            }

            // Pack them to the stream
            long currentLong = 0;
            int bitpos = 0;

            for (BlockFunction<?> block : blocksFlat) {
                int material = 0;  // 0 is null
                if (block != null) {
                    material = materials.indexOf(block.material.toString()) + 1;
                }

                currentLong |= ((long) material << bitpos);
                bitpos += bitsPerBlock;
                if ((bitpos + bitsPerBlock) > 64) {  // will the next block overflow?
                    // write the long to the stream
                    stream.writeLong(currentLong);
                    // reset the state
                    bitpos = 0;
                    currentLong = 0;
                }
            }

            // Flush
            if (bitpos >= bitsPerBlock) {
                stream.writeLong(currentLong);
            }

            StreamHelper.writeVarIntToStream(stream, blockNbt.size());
            for (Map.Entry<int[], Integer> nbt : blockNbt.entrySet()) {
                int[] pos = nbt.getKey();
                StreamHelper.writeVarIntToStream(stream, pos[0]);
                StreamHelper.writeVarIntToStream(stream, pos[1]);
                StreamHelper.writeVarIntToStream(stream, pos[2]);
                StreamHelper.writeVarIntToStream(stream, nbt.getValue());
            }
        }

        // Random blocks
        List<BlockFunction<?>> randomBlocks = blocks.stream().filter(b ->
                b instanceof BO3RandomBlockFunction || b instanceof BO4RandomBlockFunction
        ).collect(Collectors.toList());

        stream.writeBoolean(!randomBlocks.isEmpty());  // hasRandomBlocks
        if (!randomBlocks.isEmpty()) {
            Set<LocalMaterialData> localMaterialPalette = new HashSet<>();
            Set<Pair<String, NamedBinaryTag>> localMetaDataPalette = new HashSet<>();
            for (BlockFunction<?> block : randomBlocks) {
                if (block instanceof BO3RandomBlockFunction) {
                    BO3RandomBlockFunction bo3Block = (BO3RandomBlockFunction) block;
                    localMaterialPalette.addAll(Arrays.asList(bo3Block.blocks));
                    for (int i = 0; i < bo3Block.metaDataNames.length; i++) {
                        localMetaDataPalette.add(Pair.of(bo3Block.metaDataNames[i], bo3Block.metaDataTags[i]));
                    }
                } else if (block instanceof BO4RandomBlockFunction) {
                    BO4RandomBlockFunction bo4Block = (BO4RandomBlockFunction) block;
                    localMaterialPalette.addAll(Arrays.asList(bo4Block.blocks));
                    for (int i = 0; i < bo4Block.metaDataNames.length; i++) {
                        localMetaDataPalette.add(Pair.of(bo4Block.metaDataNames[i], bo4Block.metaDataTags[i]));
                    }
                }
            }

            StreamHelper.writeVarIntToStream(stream, localMaterialPalette.size());
            for (LocalMaterialData m : localMaterialPalette) {
                if (materialPalette == null) {
                    StreamHelper.writeStringToStream(stream, m.toString());
                } else {
                    StreamHelper.writeVarIntToStream(stream, materialPalette.indexOrAddMaterial(m));
                }
            }

            StreamHelper.writeVarIntToStream(stream, localMetaDataPalette.size());
            for (Pair<String, NamedBinaryTag> m : localMetaDataPalette) {
                if (metadataPalette == null) {
                    StreamHelper.writeStringToStream(stream, m.getFirst());
                } else {
                    String path = baseDir + File.separator + m.getFirst();
                    Path p = Paths.get(path);
                    String abs = p.toFile().getCanonicalPath();

                    StreamHelper.writeVarIntToStream(stream, metadataPalette.getOrRegisterNBT(abs, m.getSecond()));
                }
            }

            List<LocalMaterialData> indexableMaterials = new ArrayList<>(localMaterialPalette);
            List<String> indexableMetaData = localMetaDataPalette.stream().map(Pair::getFirst).collect(Collectors.toList());

            StreamHelper.writeVarIntToStream(stream, randomBlocks.size());
            byte type = 0;
            for (BlockFunction<?> block : randomBlocks) {
                if (block instanceof BO3RandomBlockFunction) {
                    if (type == 0) {
                        type = 3;
                        // should always be first thing written after the length
                        stream.writeByte(type);
                    }
                    if (type != 3) {
                        throw new IllegalArgumentException("Cannot have a mix of BO3 and BO4 random blocks");
                    }
                    BO3RandomBlockFunction rbf = (BO3RandomBlockFunction) block;
                    stream.writeByte(rbf.x);
                    StreamHelper.writeVarIntToStream(stream, rbf.y);
                    stream.writeByte(rbf.z);

                    stream.writeByte(rbf.blocks.length);
                    for (int i = 0; i < rbf.blocks.length; i++) {
                        byte blockChance = rbf.blockChances[i];
                        LocalMaterialData blockMaterial = rbf.blocks[i];
                        String metadataName = rbf.metaDataNames[i];

                        stream.writeByte(blockChance);
                        StreamHelper.writeVarIntToStream(stream, blockMaterial == null ? 0 : indexableMaterials.indexOf(blockMaterial) + 1);
                        StreamHelper.writeVarIntToStream(stream, metadataName == null ? 0 : indexableMetaData.indexOf(metadataName) + 1);
                    }
                } else if (block instanceof BO4RandomBlockFunction) {
                    if (type == 0) {
                        type = 4;
                        stream.writeByte(type);
                    }
                    if (type != 4) {
                        throw new IllegalArgumentException("Cannot have a mix of BO3 and BO4 random blocks");
                    }
                    BO4RandomBlockFunction rbf = (BO4RandomBlockFunction) block;
                    stream.writeByte(rbf.x);
                    StreamHelper.writeVarIntToStream(stream, rbf.y);
                    stream.writeByte(rbf.z);

                    stream.writeByte(rbf.blocks.length);
                    for (int i = 0; i < rbf.blocks.length; i++) {
                        byte blockChance = rbf.blockChances[i];
                        LocalMaterialData blockMaterial = rbf.blocks[i];
                        String metadataName = rbf.metaDataNames[i];

                        stream.writeByte(blockChance);
                        StreamHelper.writeVarIntToStream(stream, blockMaterial == null ? 0 : indexableMaterials.indexOf(blockMaterial) + 1);
                        StreamHelper.writeVarIntToStream(stream, metadataName == null ? 0 : indexableMetaData.indexOf(metadataName) + 1);
                    }
                }
            }
        }

    }
}
