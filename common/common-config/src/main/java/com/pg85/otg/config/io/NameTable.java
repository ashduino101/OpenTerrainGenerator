package com.pg85.otg.config.io;

import java.io.DataOutput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A class representing a table of setting names, used for indexed packed settings.
 * Settings and functions are combined, as there is no point in separating them.
 *
 */
public final class NameTable
{
    private final List<String> settings;
    public NameTable()
    {
        this.settings = new ArrayList<>();
    }

    void add(String name) {
        settings.add(name);
    }

    public void packToStream(DataOutput stream) throws IOException
    {
        stream.writeInt(settings.size());
        for (String item : settings)
        {
            stream.writeUTF(item);
        }
    }

    int getOrRegisterSettingId(String settingName)
    {
        if (!settings.contains(settingName))
        {
            settings.add(settingName);
        }
        return settings.indexOf(settingName);
    }
}
