package com.pg85.otg.config.io;

import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.util.helpers.StringHelper;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * A class for packing a {@link SettingsMap} to a binary file.
 * This strips away all comments and empty space, and optimizes
 * for maximum efficiency.
 *
 */
public final class FileSettingsPacker
{
    private static final int version = 1;
    public static void packToStream(SettingsMap settingsMap, DataOutput stream, ILogger logger) throws IOException
    {
        stream.writeInt(version);
        stream.writeBoolean(true);  // true -> names, false -> indices

        Collection<RawSettingValue> settings = settingsMap.getRawSettings();
        stream.writeInt(settings.size());
        for (RawSettingValue entry : settings)
        {
            packEntry(stream, entry, logger);
        }
    }

    public static void packToStream(SettingsMap settingsMap, DataOutput stream, ILogger logger, NameTable nameTable) throws IOException
    {
        stream.writeInt(version);
        stream.writeBoolean(false);

        Collection<RawSettingValue> settings = settingsMap.getRawSettings();
        stream.writeInt(settings.size());
        for (RawSettingValue entry : settings)
        {
            packEntry(stream, entry, logger, nameTable);
        }
    }


    static void packEntry(DataOutput stream, RawSettingValue entry, ILogger logger) throws IOException
    {
        stream.writeByte(entry.getType().getId());
        switch (entry.getType())
        {
            case PLAIN_SETTING:
                String[] parsed = entry.getRawValue().split(":", 2);
                stream.writeUTF(parsed[0]);
                stream.writeUTF(parsed[1]);
                break;
            case FUNCTION:
                String raw = entry.getRawValue();
                int bracketIndex = raw.indexOf('(');
                String functionName = raw.substring(0, bracketIndex);
                String parameters = raw.substring(bracketIndex + 1, raw.length() - 1);
                List<String> args = Arrays.asList(StringHelper.readCommaSeperatedString(parameters));
                stream.writeUTF(functionName);
                stream.writeShort(args.size());
                for (String item : args)
                {
                    stream.writeUTF(item);
                }
                break;
        }
    }

    static void packEntry(DataOutput stream, RawSettingValue entry, ILogger logger, NameTable nameTable) throws IOException
    {
        stream.writeByte(entry.getType().getId());
        switch (entry.getType())
        {
            case PLAIN_SETTING:
                String[] parsed = entry.getRawValue().split(":", 2);
                if (parsed[0].trim().startsWith("#"))
                {
                    logger.log(LogLevel.WARN, LogCategory.CONFIGS, String.format("Invalid setting %s", entry.getRawValue()));
                    return;
                }
                int settingIndex = nameTable.getOrRegisterSettingId(parsed[0].trim());
                stream.writeShort(settingIndex);
                stream.writeUTF(parsed[1].trim());
                break;
            case FUNCTION:
                String raw = entry.getRawValue();
                int bracketIndex = raw.indexOf('(');
                String functionName = raw.substring(0, bracketIndex).trim();
                if (functionName.trim().startsWith("#"))
                {
                    logger.log(LogLevel.WARN, LogCategory.CONFIGS, String.format("Invalid function %s", entry.getRawValue()));
                    return;
                }
                String parameters = raw.substring(bracketIndex + 1, raw.length() - 1).trim();
                List<String> args = Arrays.asList(StringHelper.readCommaSeperatedString(parameters));
                int functionIndex = nameTable.getOrRegisterSettingId(functionName);
                stream.writeShort(functionIndex);
                stream.writeShort(args.size());
                for (String item : args)
                {
                    stream.writeUTF(item);
                }
                break;
        }
    }
}
