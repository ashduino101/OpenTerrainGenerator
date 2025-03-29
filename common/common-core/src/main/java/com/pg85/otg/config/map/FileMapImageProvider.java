package com.pg85.otg.config.map;

import com.pg85.otg.interfaces.IMapImageProvider;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class FileMapImageProvider implements IMapImageProvider {
    private final File imageFile;

    public FileMapImageProvider(Path presetFolder, String imageFileName) {
        imageFile = new File(presetFolder.toFile(), imageFileName);
    }

    @Override
    public BufferedImage getBiomeMap() throws IOException {
        return ImageIO.read(imageFile);
    }
}
