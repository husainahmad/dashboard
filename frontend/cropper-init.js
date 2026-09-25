import Cropper from 'cropperjs';
import 'cropperjs/dist/cropper.min.css';

/**
 * Waiter around the Cropper.js instance used by the product image crop dialog.
 * The dialog calls init() once the source image has loaded, then crop() when
 * the user confirms. crop() returns a JPEG data URL of the selected crop area.
 */
let instance = null;

window.POSH_CROPPER = {
    init(host, aspectRatio) {
        destroy();
        const img = host.querySelector('img.cropper-source');
        if (!img) {
            return false;
        }
        instance = new Cropper(img, {
            viewMode: 1,
            dragMode: 'crop',
            aspectRatio: Number(aspectRatio) > 0 ? Number(aspectRatio) : NaN,
            autoCropArea: 1,
            restore: false,
            checkCrossOrigin: false,
            background: true,
            guides: true,
            center: true,
            highlight: false,
            responsive: true,
        });
        return true;
    },

    crop(host, aspectRatio, quality) {
        if (!instance) {
            init(host, aspectRatio);
        }
        if (!instance) {
            return null;
        }
        const canvas = instance.getCroppedCanvas({
            maxWidth: 1600,
            maxHeight: 1600,
            imageSmoothingQuality: 'high',
        });
        const dataUrl = canvas.toDataURL('image/jpeg', quality);
        destroy();
        return dataUrl;
    },
};

function destroy() {
    if (instance) {
        instance.destroy();
        instance = null;
    }
}