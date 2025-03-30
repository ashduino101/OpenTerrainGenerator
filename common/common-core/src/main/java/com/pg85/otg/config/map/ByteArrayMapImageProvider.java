package com.pg85.otg.config.map;

import com.pg85.otg.interfaces.IMapImageProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ByteArrayMapImageProvider implements IMapImageProvider {
    private final byte[] data;
    BufferedImage cachedImage;

    public ByteArrayMapImageProvider(byte[] imageData) {
        this.data = imageData;
        this.cachedImage = null;
    }

    @Override
    public BufferedImage getBiomeMap() throws IOException {
        if (this.cachedImage != null) return this.cachedImage;
        this.cachedImage = ImageIO.read(new ByteArrayInputStream(this.data));
        return this.cachedImage;
    }
}
