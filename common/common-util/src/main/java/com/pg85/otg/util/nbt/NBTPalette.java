package com.pg85.otg.util.nbt;

import com.pg85.otg.util.StringTable;

import java.io.*;
import java.util.HashMap;

/// Used for packing NBT data to packed presets.
/// Does NOT preserve original filenames.
public class NBTPalette {
    private StringTable names;
    private HashMap<String, NamedBinaryTag> nameToNbt;

    public NBTPalette() {
        this.nameToNbt = new HashMap<>();
        this.names = new StringTable();
    }

    public int getOrRegisterNBT(String fullPath, NamedBinaryTag nbt) {
        this.nameToNbt.putIfAbsent(fullPath, nbt);
        return this.names.getOrRegisterString(fullPath);
    }

    public int add(String fullPath, NamedBinaryTag nbt) {
        this.nameToNbt.putIfAbsent(fullPath, nbt);
        return this.names.getOrRegisterString(fullPath);
    }

    public int get(String fullPath) {
        return this.names.getOrRegisterString(fullPath);
    }

    public String getNameFromIndex(int idx) {
        return this.names.getStringById(idx);
    }

    public NamedBinaryTag getNBTFromName(String name) {
        return this.nameToNbt.get(name);
    }

    public void packToStream(DataOutput stream) throws IOException
    {
        stream.writeInt(nameToNbt.size());
        for (String item : names.getAllStoredStrings())
        {
            NamedBinaryTag tag = this.nameToNbt.get(item);
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

        StringTable table = new StringTable();

        int numNBT = stream.readInt();
        for (int i = 0; i < numNBT; i++)
        {
            String name = "packed$" + (i + 1) + ".nbt";
            table.getOrRegisterString(name);
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
            palette.nameToNbt.put(name, tag);
        }

        palette.names = table;

        return palette;
    }
}
