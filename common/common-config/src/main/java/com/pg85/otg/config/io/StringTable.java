package com.pg85.otg.config.io;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A class representing a table of strings, used for indexed packed settings.
 * Settings and functions are combined, as there is no point in separating them.
 *
 */
public final class StringTable
{
    private final List<String> strings;
    public StringTable()
    {
        this.strings = new ArrayList<>();
    }

    void add(String name) {
        strings.add(name);
    }

    public void packToStream(DataOutput stream) throws IOException
    {
        stream.writeInt(strings.size());
        for (String item : strings)
        {
            stream.writeUTF(item);
        }
    }

    public static StringTable readFromStream(DataInput stream) throws IOException
    {
        StringTable table = new StringTable();

        int numNames = stream.readInt();
        for (int i = 0; i < numNames; i++)
        {
            table.strings.add(stream.readUTF());
        }

        return table;
    }

    int getOrRegisterString(String value)
    {
        if (!strings.contains(value))
        {
            strings.add(value);
        }
        return strings.indexOf(value);
    }

    String getStringById(int id) {
        return strings.get(id);
    }
}
