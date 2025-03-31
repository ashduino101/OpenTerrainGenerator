package com.pg85.otg.util.materials;

import com.pg85.otg.util.helpers.StreamHelper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;

public class MaterialPalette {
    // TODO: for some reason comparing LocalMaterialData doesn't work?
    LinkedList<String> materials;
    LinkedList<String> tags;

    public MaterialPalette() {
        this.materials = new LinkedList<>();
        this.tags = new LinkedList<>();
    }

    public int indexOrAddMaterial(LocalMaterialData material) {
        return this.indexOrAddMaterial(material.toString());
    }

    public int indexOrAddMaterial(String materialString) {
        if (this.materials.contains(materialString)) {
            return this.materials.indexOf(materialString);
        }
        this.materials.add(materialString);
        return this.materials.size() - 1;
    }

    public int indexOrAddTag(String tagString) {
        if (this.tags.contains(tagString)) {
            return this.tags.indexOf(tagString);
        }
        this.tags.add(tagString);
        return this.tags.size() - 1;
    }

    // We shouldn't have to cache here since
    // LocalMaterialReader caches already
    public String getMaterial(int index) {
        return this.materials.get(index);
    }

    public String getTag(int index) {
        // We shouldn't have to cache here since
        // LocalMaterialReader caches already
        return this.tags.get(index);
    }

    public void writeToStream(DataOutput stream) throws IOException {
        StreamHelper.writeVarIntToStream(stream, this.materials.size());
        for (String material : this.materials) {
            stream.writeUTF(material);
        }

        StreamHelper.writeVarIntToStream(stream, this.tags.size());
        for (String tag : this.tags) {
            stream.writeUTF(tag);
        }
    }

    public static MaterialPalette readFromStream(DataInput stream) throws IOException {
        MaterialPalette palette = new MaterialPalette();

        int numMaterials = StreamHelper.readVarIntFromStream(stream);
        for (int i = 0; i < numMaterials; i++) {
            palette.materials.add(stream.readUTF());
        }

        int numTags = StreamHelper.readVarIntFromStream(stream);
        for (int i = 0; i < numTags; i++) {
            palette.tags.add(stream.readUTF());
        }

        return palette;
    }
}
