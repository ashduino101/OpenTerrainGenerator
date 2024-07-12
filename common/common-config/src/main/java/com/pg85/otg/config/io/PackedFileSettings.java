package com.pg85.otg.config.io;

import com.pg85.otg.exceptions.InvalidConfigException;
import com.pg85.otg.interfaces.ILogger;
import com.pg85.otg.util.helpers.StringHelper;
import com.pg85.otg.util.logging.LogCategory;
import com.pg85.otg.util.logging.LogLevel;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A class for packing a {@link SettingsMap} to a binary file.
 * This strips away all comments and empty space, and optimizes
 * for maximum efficiency.
 *
 */
public final class PackedFileSettings
{
    private static final int version = 1;
    public static void packToStream(SettingsMap settingsMap, DataOutput stream, ILogger logger) throws IOException
    {
        stream.writeInt(version);
        stream.writeBoolean(true);  // true -> names, false -> indices

        Collection<RawSettingValue> settings = settingsMap.getRawSettings()
                .stream()
                .filter(e -> e.getType() != RawSettingValue.ValueType.PLAIN_SETTING && e.getType() != RawSettingValue.ValueType.FUNCTION)
                .collect(Collectors.toList());

        stream.writeInt(settings.size());
        for (RawSettingValue entry : settings)
        {
            if (entry.getType() != RawSettingValue.ValueType.PLAIN_SETTING && entry.getType() != RawSettingValue.ValueType.FUNCTION) {
                continue;
            }
            packEntry(stream, entry, logger);
        }
    }

    public static void packToStream(SettingsMap settingsMap, DataOutput stream, ILogger logger, NameTable nameTable) throws IOException
    {
        stream.writeInt(version);
        stream.writeBoolean(false);

        Collection<RawSettingValue> settings = settingsMap.getRawSettings()
                .stream()
                .filter(e -> e.getType() != RawSettingValue.ValueType.PLAIN_SETTING && e.getType() != RawSettingValue.ValueType.FUNCTION)
                .collect(Collectors.toList());

        stream.writeInt(settings.size());
        for (RawSettingValue entry : settings)
        {
            packEntry(stream, entry, logger, nameTable);
        }
    }


    private static void packEntry(DataOutput stream, RawSettingValue entry, ILogger logger) throws IOException
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

    private static void packEntry(DataOutput stream, RawSettingValue entry, ILogger logger, NameTable nameTable) throws IOException
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

    public static SettingsMap readFromStream(DataInputStream stream, ILogger logger, NameTable nameTable) throws IOException, InvalidConfigException {
        int dataVersion = stream.readInt();
        if (dataVersion > version) {
            throw new InvalidConfigException(String.format("Settings version too new! Supports up to %d (found %d)", version, dataVersion));
        }
        boolean hasNames = stream.readBoolean();

        SettingsMap map = new SimpleSettingsMap("<packed>");
        int numEntries = stream.readInt();
        for (int i = 0; i < numEntries; i++) {
            if (hasNames) {
                readEntry(stream, map, logger);
            } else {
                readEntry(stream, map, logger, nameTable);
            }
        }

        return map;
    }

    private static void readEntry(DataInputStream stream, SettingsMap map, ILogger logger, NameTable table) throws IOException {
        switch (stream.readByte()) {
            case 1:  // plain setting
                String name = table.getNameById(stream.readShort());
//                short valueLen = stream.readShort();
//                // FIXME: for some reason readUTF doesn't work here
//                byte[] buf = new byte[valueLen];
//                stream.read(buf);
//                String value = new String(buf);
                String value = stream.readUTF();
                map.addRawSetting(RawSettingValue.create(RawSettingValue.ValueType.PLAIN_SETTING, String.format("%s: %s", name, value)));
                break;
            case 2:  // function
                String funcName = table.getNameById(stream.readShort());
                short numArgs = stream.readShort();
                List<String> args = new ArrayList<>();
                for (int i = 0; i < numArgs; i++) {
                    args.add(stream.readUTF());
                }
                map.addRawSetting(RawSettingValue.create(RawSettingValue.ValueType.FUNCTION, String.format("%s(%s)", funcName, String.join(",", args))));
                break;
        }
    }

    private static void readEntry(DataInputStream stream, SettingsMap map, ILogger logger) throws IOException {
        switch (stream.readByte()) {
            case 1:  // plain setting
                String name = stream.readUTF();
                String value = stream.readUTF();
                map.addRawSetting(RawSettingValue.create(RawSettingValue.ValueType.PLAIN_SETTING, String.format("%s: %s", name, value)));
                break;
            case 2:  // function
                String funcName = stream.readUTF();
                short numArgs = stream.readShort();
                List<String> args = new ArrayList<>();
                for (int i = 0; i < numArgs; i++) {
                    args.add(stream.readUTF());
                }
                map.addRawSetting(RawSettingValue.create(RawSettingValue.ValueType.FUNCTION, String.format("%s(%s)", funcName, String.join(",", args))));
                break;
        }
    }
}
