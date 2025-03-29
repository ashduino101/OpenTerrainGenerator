package com.pg85.otg.interfaces;

import java.awt.image.BufferedImage;
import java.io.IOException;

public interface IMapImageProvider {
    BufferedImage getBiomeMap() throws IOException;
}
