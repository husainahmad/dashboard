package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.ImageDto;
import com.harmoni.menu.dashboard.dto.ProductImageDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ImageUtil;
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
import com.vaadin.flow.server.StreamResource;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;

/**
 * Uploads a product image to the REST API and shows it as a compact thumbnail.
 * The image is uploaded immediately when the user selects a file, and the
 * resulting {@link ProductImageDto} is stored in this view for later retrieval
 * by the owner form.
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
    private final Button removeButton = new Button("Remove", new Icon(VaadinIcon.TRASH));
    private final Icon placeholderIcon = VaadinIcon.PICTURE.create();
    private final Span imageTitle = new Span("Product image");
    private final Span imageHint = new Span("PNG, JPG or GIF");

    private void renderLayout() {
        setWidthFull();
        setPadding(false);
        setSpacing(false);

        image.getStyle().set("width", "96px")
                .set("height", "96px")
                .set("object-fit", "cover")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("background", "var(--lumo-contrast-5pct)");
        image.setVisible(false);

        placeholderIcon.setSize("28px");
        placeholderIcon.setColor("var(--lumo-contrast-30pct)");
        imageTitle.getStyle().set("font-weight", "600");
        imageHint.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(MimeTypeUtils.IMAGE_JPEG_VALUE, MimeTypeUtils.IMAGE_GIF_VALUE, MimeTypeUtils.IMAGE_PNG_VALUE);
        upload.setMaxFiles(1);
        upload.setDropAllowed(true);
        upload.setDropLabel(new Span("or drag & drop here"));
        Button browseButton = new Button("Browse image", new Icon(VaadinIcon.FOLDER_OPEN));
        browseButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        upload.setUploadButton(browseButton);

        removeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        removeButton.setVisible(false);
        removeButton.addClickListener(event -> clearImage());

        upload.addSucceededListener(succeededEvent -> {

            ImageDto imageDto = ImageDto.builder()
                    .fileName(buffer.getFileName())
                    .mimeType(succeededEvent.getMIMEType())
                    .fileStream(buffer.getInputStream())
                    .build();

            try {
                if (ObjectUtils.isNotEmpty(productTreeItem)) {
                    restClientMenuService.uploadUpdatedProduct(productTreeItem.getProductId(), imageDto)
                            .subscribe(this::processResponse);
                } else {
                    restClientMenuService.uploadProduct(imageDto).subscribe(this::processResponse);
                }
            } catch (IOException e) {
                log.error("Error", e);
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
     * Processes the REST API response after uploading an image. If the response
     * contains image data, it converts it to a {@link ProductImageDto} and updates
     * the UI to display the uploaded image.
     *
     * @param restAPIResponse the response from the REST API after uploading the image
     */
    private void processResponse(RestAPIResponse restAPIResponse) {
        if (ObjectUtils.isNotEmpty(restAPIResponse.getData())) {
            productImageDto = ObjectUtil.convertValueToObject(restAPIResponse.getData(), ProductImageDto.class);
            UiUtil.safeAccess(ui, () -> setImage(ImageUtil.createStreamResource(productImageDto.getImageBlob(),
                    productImageDto.getFileName())));
        }
    }

    /**
     * Shows the given image as the compact product thumbnail. Call this from a
     * UI-access context.
     */
    public void setImage(StreamResource resource) {
        image.setSrc(resource);
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