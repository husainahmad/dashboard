package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.ImageDto;
import com.harmoni.menu.dashboard.dto.ProductImageDto;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ImageUtil;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;

@RequiredArgsConstructor
@Route("product-image-upload")
@Slf4j
public class ProductImageUploadView extends HorizontalLayout {

    private final RestClientMenuService restClientMenuService;
    private final UI ui;
    private final transient ProductTreeItem productTreeItem;

    @Getter
    private transient ProductImageDto productImageDto;
    @Getter
    Image image = new Image();

    private void renderLayout() {
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(MimeTypeUtils.IMAGE_JPEG_VALUE, MimeTypeUtils.IMAGE_PNG_VALUE);
        add(upload);

        add(image);

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
    }

    private void processResponse(RestAPIResponse restAPIResponse) {
        if (ObjectUtils.isNotEmpty(restAPIResponse.getData())) {
            productImageDto = ObjectUtil.convertValueToObject(restAPIResponse.getData(), ProductImageDto.class);
            ui.access(() -> {
                image.setSrc(ImageUtil.createStreamResource(productImageDto.getImageBlob(),
                        productImageDto.getFileName()));
                image.setMaxWidth("300px");
            });
        }
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        renderLayout();
    }
}
