package com.pg85.otg.util.nbt;

import java.io.*;
import java.util.*;

/// Used for packing NBT data to packed presets.
/// Does NOT preserve original filenames.
public class NBTPalette {
    private final Map<Integer, NamedBinaryTag> nameHashToNbt;
    private final List<Integer> hashes;

    public NBTPalette() {
        this.nameHashToNbt = new HashMap<>();
        this.hashes = new LinkedList<>();
    }

    public int getOrRegisterNBT(String fullPath, NamedBinaryTag nbt) {
        int nameHash = fullPath.hashCode();
        int idx = this.hashes.indexOf(nameHash);
        if (idx == -1) {
            this.nameHashToNbt.put(nameHash, nbt);
            this.hashes.add(nameHash);
            return this.hashes.size() - 1;
        }
        return idx;
    }

    public int indexOf(String fullPath) {
        return this.hashes.indexOf(fullPath.hashCode());
    }

    public int getHashFromIndex(int idx) {
        return this.hashes.get(idx);
    }

    public NamedBinaryTag getNBTFromNameHash(int nameHash) {
        return this.nameHashToNbt.get(nameHash);
    }

    public void packToStream(DataOutput stream) throws IOException
    {
        stream.writeInt(nameHashToNbt.size());
        for (int item : hashes)
        {
            NamedBinaryTag tag = this.nameHashToNbt.get(item);
            // FIXME: how does this happen?
            stream.writeBoolean(tag == null);
            if (tag == null) continue;

            ByteArrayOutputStream nbtStream = new ByteArrayOutputStream();
            tag.writeTo(nbtStream, false);
            byte[] arr = nbtStream.toByteArray();
            stream.writeInt(arr.length);
            stream.write(arr);
        }
    }

    public static NBTPalette readFromStream(DataInput stream) throws IOException
    {
        NBTPalette palette = new NBTPalette();

        int numNBT = stream.readInt();
        for (int i = 0; i < numNBT; i++)
        {
            String name = "packed$" + (i + 1) + ".nbt";
            boolean isNull = stream.readBoolean();
            NamedBinaryTag tag;
            if (isNull) {
                tag = null;
            } else {
                int len = stream.readInt();
                byte[] buf = new byte[len];
                stream.readFully(buf);
                tag = NamedBinaryTag.readFrom(new ByteArrayInputStream(buf), false);
            }
            int nameHash = name.hashCode();
            palette.nameHashToNbt.put(nameHash, tag);
            palette.hashes.add(nameHash);
        }

        return palette;
    }
}
