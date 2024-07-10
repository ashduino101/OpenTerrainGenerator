package com.pg85.otg.customobject.bo4;

import com.pg85.otg.util.nbt.NamedBinaryTag;

import java.util.ArrayList;

public class BO4NBTPacker
{
    BO4 object;

    public BO4NBTPacker(BO4 bo4)
    {
        this.object = bo4;
    }

    static void writeTag(NamedBinaryTag root, NamedBinaryTag.Type type, String name, Object value) {
        if (value != null)
        {
            root.addTag(new NamedBinaryTag(NamedBinaryTag.Type.TAG_String, name, value));
        }
    }

    public void pack() {
        BO4Config config = this.object.getConfig();
        NamedBinaryTag rootTag = new NamedBinaryTag(NamedBinaryTag.Type.TAG_Compound, "", new ArrayList<NamedBinaryTag>());
        writeTag(rootTag, NamedBinaryTag.Type.TAG_String, "Author", config.author);
        writeTag(rootTag, NamedBinaryTag.Type.TAG_String, "Description", config.description);
        writeTag(rootTag, NamedBinaryTag.Type.TAG_Byte, "DoReplaceBlocks", config.doReplaceBlocks);
        writeTag(rootTag, NamedBinaryTag.Type.TAG_Int, "Frequency", config.frequency);
        writeTag(rootTag, NamedBinaryTag.Type.TAG_Byte, "FixedRotation", config.fixedRotation);
        writeTag(rootTag, NamedBinaryTag.Type.TAG_Int, "MinHeight", config.minHeight);
        writeTag(rootTag, NamedBinaryTag.Type.TAG_Int, "MaxHeight", config.maxHeight);
        writeTag(rootTag, NamedBinaryTag.Type.TAG_Byte, "SpawnHeight", config.spawnHeight);
        writeTag(rootTag, NamedBinaryTag.Type.TAG_Byte, "UseCenterForHighestBlock", config.useCenterForHighestBlock);

        NamedBinaryTag inheritTag = new NamedBinaryTag(NamedBinaryTag.Type.TAG_List, "InheritBO3s", NamedBinaryTag.Type.TAG_String);


        writeTag(rootTag, NamedBinaryTag.Type.TAG_List, "InheritBO3", String.join(",", config.getInheritedBO3s()));
        writeTag(rootTag, NamedBinaryTag.Type.TAG_Byte, "InheritBO3Rotation", config.inheritBO3Rotation);
    }
}
