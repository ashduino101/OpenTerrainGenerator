package com.pg85.otg.config.map;

import com.pg85.otg.interfaces.IMapImageProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

public class ByteArrayMapImageProvider implements IMapImageProvider {
    private final byte[] data;

    public ByteArrayMapImageProvider(byte[] imageData) {
        this.data = imageData;
    }

    @Override
    public BufferedImage getBiomeMap() throws IOException {
        return ImageIO.read(new ByteArrayInputStream(this.data));
    }
}
