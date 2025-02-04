package com.harmoni.menu.dashboard.util;

import com.harmoni.menu.dashboard.dto.ImageDto;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class ImageUtil {

    private ImageUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static File convertImageDtoToFile(ImageDto imageDto) throws IOException {
        File tempFile = File.createTempFile("temp_".concat(UUID.randomUUID().toString()).concat("_"), imageDto.getFileName());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(imageDto.getFileStream().readAllBytes()); // Assuming imageDto has byte[] data
        }
        return tempFile;
    }
}
