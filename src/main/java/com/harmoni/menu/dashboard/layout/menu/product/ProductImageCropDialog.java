package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.server.StreamResource;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.function.Consumer;

/**
 * Interactive image crop dialog backed by Cropper.js. Receives the raw picked
 * image bytes, lets the user drag the crop area (locked to the given aspect
 * ratio, e.g. square for product thumbnails) and hands the cropped JPEG bytes
 * back through {@code onCropped} when the user confirms.
 */
@JsModule("./cropper-init.js")
public class ProductImageCropDialog extends Dialog {

    private static final double CROP_QUALITY = 0.92;

    private final byte[] sourceBytes;
    private final String fileName;
    private final double aspectRatio;
    private final Consumer<byte[]> onCropped;

    private final Div host = new Div();
    private final Image sourceImage = new Image();

    /**
     * @param sourceBytes the picked file bytes rendered for cropping
     * @param fileName    the original file name, kept for the preview resource
     * @param aspectRatio the fixed crop ratio (e.g. {@code 1.0} square, or
     *                    {@code -1} for a free-form crop area)
     * @param onCropped   callback receiving the cropped JPEG bytes
     */
    public ProductImageCropDialog(byte[] sourceBytes, String fileName, double aspectRatio,
                                  Consumer<byte[]> onCropped) {
        this.sourceBytes = sourceBytes;
        this.fileName = fileName;
        this.aspectRatio = aspectRatio;
        this.onCropped = onCropped;

        addClassName("cropper-dialog");
        setHeaderTitle(Messages.get("label.cropImage"));
        setWidth("760px");

        sourceImage.addClassName("cropper-source");
        host.addClassName("cropper-host");
        host.add(sourceImage);

        Button cancelButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        Button confirmButton = new Button(Messages.get("action.cropUpload"));
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        confirmButton.addClickListener(event -> cropAndApply());

        getFooter().add(cancelButton, confirmButton);
        add(host);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        sourceImage.setSrc(new StreamResource(fileName, () -> new ByteArrayInputStream(sourceBytes)));
        initCropper();
    }

    private void initCropper() {
        host.getElement().executeJs(
                "return new Promise((resolve) => {" +
                        "  const img = $0.querySelector('img.cropper-source');" +
                        "  const start = () => { window.POSH_CROPPER.init($0, $1); resolve(true); };" +
                        "  if (img.complete && img.naturalWidth > 0) { start(); }" +
                        "  else { img.addEventListener('load', start, { once: true });" +
                        "         img.addEventListener('error', () => resolve(false), { once: true }); }" +
                        "})",
                host.getElement(), aspectRatio);
    }

    private void cropAndApply() {
        host.getElement().executeJs(
                        "return window.POSH_CROPPER.crop($0, $1, $2);",
                        host.getElement(), aspectRatio, CROP_QUALITY)
                .then(String.class, dataUrl -> {
                    if (dataUrl == null || dataUrl.isBlank() || !dataUrl.startsWith("data:image")) {
                        return;
                    }
                    close();
                    onCropped.accept(decodeBase64(dataUrl));
                });
    }

    private static byte[] decodeBase64(String dataUrl) {
        return Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
    }
}