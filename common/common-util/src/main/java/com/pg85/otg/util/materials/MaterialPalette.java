package com.pg85.otg.util.materials;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.LinkedList;

public class MaterialPalette {
    // TODO: for some reason comparing LocalMaterialData doesn't work?
    LinkedList<String> materials;

    private static final int materialLimit = 1 << 16;

    public MaterialPalette() {
        this.materials = new LinkedList<>();
    }

    public int indexOrAdd(LocalMaterialData material) {
        return this.indexOrAdd(material.toString());
    }

    public int indexOrAdd(String materialString) {
        if (this.materials.contains(materialString)) {
            return this.materials.indexOf(materialString);
        }
        this.materials.add(materialString);
        if (this.materials.size() > materialLimit) {
            throw new ArrayStoreException("Cannot store more than " + materialLimit + " blocks in one palette");
        }
        return this.materials.size() - 1;
    }

    public String getMaterial(int index) {
        return this.materials.get(index);
    }

    public void writeToStream(DataOutput stream) throws IOException {
        stream.writeInt(this.materials.size());
        for (String material : this.materials) {
            stream.writeUTF(material);
        }
    }

    public static MaterialPalette readFromStream(DataInput stream) throws IOException {
        int numMaterials = stream.readInt();
        MaterialPalette palette = new MaterialPalette();
        for (int i = 0; i < numMaterials; i++) {
            palette.materials.add(stream.readUTF());
        }
        return palette;
    }
}
