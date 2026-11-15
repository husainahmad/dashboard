package com.harmoni.menu.dashboard.util;

import com.harmoni.menu.dashboard.dto.ImageDto;

import java.io.*;
import java.util.UUID;

/**
 * File helpers for product images.
 *
 * <p>Converts an uploaded {@link ImageDto} into a temp file for the upload
 * request.</p>
 */
public class ImageUtil {

    private ImageUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static File convertImageDtoToFile(ImageDto imageDto) throws IOException {
        File tempFile = File.createTempFile("".concat(UUID.randomUUID().toString()).concat("_"), imageDto.getFileName());
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(imageDto.getFileStream().readAllBytes()); // Assuming imageDto has byte[] data
        }
        return tempFile;
    }
}
