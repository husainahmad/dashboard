package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.ImageDto;
import com.harmoni.menu.dashboard.dto.ProductImageDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Uploads a product image to the REST API and shows it as a compact thumbnail.
 * The image is uploaded immediately when the user selects a file, and the
 * resulting {@link ProductImageDto} is stored in this view for later retrieval
 * by the owner form. The menu service hosts the image on ImgBB and returns the
 * hosted URL in {@link ProductImageDto#url}.
 */
@RequiredArgsConstructor
@Route("product-image-upload")
@Slf4j
public class ProductImageUploadView extends VerticalLayout {

    private final RestClientMenuService restClientMenuService;
    private final UI ui;
    private final transient ProductTreeItem productTreeItem;

    @Getter
    private transient ProductImageDto productImageDto;
    @Getter
    Image image = new Image();
    private final Button removeButton = new Button(Messages.get("action.remove"), new Icon(VaadinIcon.TRASH));
    private final Icon placeholderIcon = VaadinIcon.PICTURE.create();
    private final Span imageTitle = new Span(Messages.get("label.productImage"));
    private final Span imageHint = new Span(Messages.get("label.imageHint"));

    private void renderLayout() {
        setWidthFull();
        setPadding(false);
        setSpacing(false);

        image.getStyle().set(Css.WIDTH, "96px")
                .set("height", "96px")
                .set("object-fit", "cover")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("background", "var(--lumo-contrast-5pct)");
        image.setVisible(false);

        placeholderIcon.setSize("28px");
        placeholderIcon.setColor("var(--lumo-contrast-30pct)");
        imageTitle.getStyle().set(Css.FONT_WEIGHT, "600");
        imageHint.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(MimeTypeUtils.IMAGE_JPEG_VALUE, MimeTypeUtils.IMAGE_GIF_VALUE, MimeTypeUtils.IMAGE_PNG_VALUE);
        upload.setMaxFiles(1);
        upload.setDropAllowed(true);
        upload.setDropLabel(new Span(Messages.get("label.dropHere")));
        Button browseButton = new Button(Messages.get("action.browseImage"), new Icon(VaadinIcon.FOLDER_OPEN));
        browseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        upload.setUploadButton(browseButton);

        removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        removeButton.setVisible(false);
        removeButton.addClickListener(event -> clearImage());

        upload.addSucceededListener(succeededEvent -> {
            try {
                byte[] pickedBytes = buffer.getInputStream().readAllBytes();
                if (pickedBytes.length == 0) {
                    return;
                }
                new ProductImageCropDialog(pickedBytes, buffer.getFileName(), 1.0,
                        croppedBytes -> uploadImage(croppedBytes, buffer.getFileName())).open();
            } catch (IOException e) {
                log.error("Error reading picked image", e);
            }
        });

        HorizontalLayout actions = new HorizontalLayout(upload, removeButton);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        VerticalLayout tile = new VerticalLayout();
        tile.addClassName("upload-tile");
        tile.setWidthFull();
        tile.setPadding(true);
        tile.setSpacing(true);
        tile.setAlignItems(FlexComponent.Alignment.CENTER);
        tile.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        tile.add(image, placeholderIcon, imageTitle, imageHint, actions);

        add(tile);
    }

    /**
     * Uploads the cropped image returned by {@link ProductImageCropDialog}. The
     * crop is always a JPEG, so the file name is normalised to a {@code .jpg}
     * suffix regardless of the picked format.
     *
     * @param croppedBytes the JPEG bytes produced by the crop dialog
     * @param pickedName   the original file name from the upload
     */
    private void uploadImage(byte[] croppedBytes, String pickedName) {
        try {
            ImageDto imageDto = ImageDto.builder()
                    .fileName(pickedName.replaceAll("(?i)\\.[a-z0-9]+$", "") + ".jpg")
                    .mimeType(MimeTypeUtils.IMAGE_JPEG_VALUE)
                    .fileStream(new ByteArrayInputStream(croppedBytes))
                    .build();
            if (ObjectUtils.isNotEmpty(productTreeItem)) {
                restClientMenuService.uploadUpdatedProduct(productTreeItem.getProductId(), imageDto)
                        .subscribe(this::processResponse);
            } else {
                restClientMenuService.uploadProduct(imageDto).subscribe(this::processResponse);
            }
        } catch (IOException e) {
            log.error("Error uploading cropped image", e);
        }
    }

    /**
     * Processes the REST API response after uploading an image. If the response
     * contains image data, it converts it to a {@link ProductImageDto} and updates
     * the UI to display the uploaded image.
     *
     * @param restAPIResponse the response from the REST API after uploading the image
     */
    private void processResponse(RestAPIResponse restAPIResponse) {
        if (ObjectUtils.isNotEmpty(restAPIResponse.getData())) {
            productImageDto = ObjectUtil.convertValueToObject(restAPIResponse.getData(), ProductImageDto.class);
            UiUtil.safeAccess(ui, () -> setImage(productImageDto.getUrl()));
        }
    }

    /**
     * Shows the given image URL as the compact product thumbnail. Call this from
     * a UI-access context.
     */
    public void setImage(String imageUrl) {
        image.setSrc(imageUrl);
        showImageState(true);
        removeButton.setVisible(true);
    }

    private void clearImage() {
        image.setSrc("");
        showImageState(false);
        removeButton.setVisible(false);
        productImageDto = null;
    }

    /**
     * Updates the visibility of the image and placeholder elements based on
     * whether an image is present.
     *
     * @param hasImage {@code true} if an image is present, {@code false} otherwise
     */
    private void showImageState(boolean hasImage) {
        image.setVisible(hasImage);
        placeholderIcon.setVisible(!hasImage);
        imageTitle.setVisible(!hasImage);
        imageHint.setVisible(!hasImage);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        renderLayout();
    }
}