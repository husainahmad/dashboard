package com.harmoni.menu.dashboard.util;

import com.harmoni.menu.dashboard.dto.ImageDto;
import com.vaadin.flow.server.StreamResource;

import java.io.*;
import java.util.UUID;

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

    public static StreamResource createStreamResource(byte[] imageBytes, String fileName) {
        return new StreamResource(fileName,
                () -> new ByteArrayInputStream(imageBytes));
    }
}
