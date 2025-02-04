package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.ImageDto;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;

@RequiredArgsConstructor
@Route("product-image-upload")
@Slf4j
public class ProductImageUploadView extends VerticalLayout {

    private final RestClientMenuService restClientMenuService;

    private void renderLayout() {
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(MimeTypeUtils.IMAGE_JPEG_VALUE, MimeTypeUtils.IMAGE_PNG_VALUE);

        upload.addSucceededListener(succeededEvent -> {
            ImageDto imageDto = ImageDto.builder()
                    .fileName(buffer.getFileName())
                    .mimeType(succeededEvent.getMIMEType())
                    .fileStream(buffer.getInputStream())
                    .build();
            try {
                restClientMenuService.uploadProduct(imageDto).subscribe(restAPIResponse ->
                        log.debug("restAPIResponse : {}", restAPIResponse));
            } catch (IOException e) {
                log.error("Error", e);
            }
        });
        add(upload);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        renderLayout();
    }
}
