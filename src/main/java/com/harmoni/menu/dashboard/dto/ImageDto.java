package com.harmoni.menu.dashboard.dto;

import lombok.Builder;
import lombok.Data;

import java.io.InputStream;
import java.io.Serializable;

/**
 * Carries an uploaded product image to the image upload endpoints.
 *
 * <p>Used as the payload for {@code /api/v1/product/image/upload} and
 * {@code /api/v1/product/image/{id}/upload}. The image bytes travel in
 * {@link #fileStream}, which is {@code transient} and therefore never
 * serialized on the wire. The menu service hosts the image on ImgBB and
 * returns the hosted URL in {@link ProductImageDto#url}.</p>
 */

@Data
@Builder
public class ImageDto implements Serializable  {

    /** The image id, or {@code null} when the image has not been saved yet. */

    private Integer id;

    /** The binary content of the uploaded image; {@code null} when no upload is in progress. */

    private transient InputStream fileStream;

    /** The file name, used to derive the upload temp file and the download URL. */

    private String fileName;

    /** The content type of the uploaded image, such as {@code image/png}. */

    private String mimeType;
}
