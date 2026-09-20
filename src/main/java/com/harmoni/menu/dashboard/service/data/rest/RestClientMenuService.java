package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.util.ImageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Blocking REST client for the menu endpoints: categories, brands, tiers,
 * products (including image upload) and customizations, plus the per-product
 * customization links. Builds URLs from {@link MenuProperties}, extends
 * {@link RestClientService} and returns {@link Mono} responses.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class RestClientMenuService extends RestClientService implements Serializable {

    private final transient MenuProperties urlMenuProperties;
    private static final String FORMAT_STRING = "%s/%d";

    /**
     * Creates a menu category.
     *
     * @param categoryDto the category to create
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> createCategory(CategoryDto categoryDto) {
        return post(urlMenuProperties.getUrl().getCategory(), Mono.just(categoryDto), CategoryDto.class);
    }

    /**
     * Lists all categories of a brand.
     *
     * @param brandId the owning brand id
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> getAllCategoryByBrand(Integer brandId) {
        return get(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getCategories().getBrand(), brandId));
    }

    /**
     * Lists all tiers of the given type for a brand.
     *
     * @param brandId the owning brand id
     * @param type    the tier type
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> getAllTierByBrand(Integer brandId, String type) {
        return get("%s/brand/%d/type/%s".formatted(urlMenuProperties.getUrl().getTier(), brandId, type));
    }

    /**
     * Lists all brands.
     *
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> getAllBrand() {
        return get(urlMenuProperties.getUrl().getBrand());
    }

    /**
     * Fetches a single product.
     *
     * @param productId the product id
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> getProduct(Integer productId) {
        return get(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getProduct(), productId));
    }

    /**
     * Creates a product.
     *
     * @param productDto the product to create
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> saveProduct(ProductDto productDto) {
        return post(urlMenuProperties.getUrl().getProduct(),
                Mono.just(productDto),  ProductDto.class);
    }

    /**
     * Updates an existing product.
     *
     * @param productDto the product to update
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateProduct(ProductDto productDto) {
        return put(urlMenuProperties.getUrl().getProduct(),
                Mono.just(productDto),  ProductDto.class);
    }

    /**
     * Uploads the image of a product.
     *
     * @param imageDto the image to upload
     * @return a {@link Mono} with the server response
     * @throws IOException when the image cannot be written to a file
     */
    public Mono<RestAPIResponse> uploadProduct(ImageDto imageDto) throws IOException {
        File file = ImageUtil.convertImageDtoToFile(imageDto);
        return upload(urlMenuProperties.getUrl().getProducts().getImages().getUpload(),
                file);
    }

    /**
     * Replaces the image of an existing product.
     *
     * @param productId the product id
     * @param imageDto  the new image
     * @return a {@link Mono} with the server response
     * @throws IOException when the image cannot be written to a file
     */
    public Mono<RestAPIResponse> uploadUpdatedProduct(Integer productId, ImageDto imageDto) throws IOException {
        File file = ImageUtil.convertImageDtoToFile(imageDto);
        return uploadUpdate(urlMenuProperties.getUrl().getProducts().getImages().getUploadUpdate().formatted(productId), file);
    }

    /**
     * Deletes a product.
     *
     * @param productDto the product to delete
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> deleteProduct(ProductDto productDto) {
        return delete(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getProduct(), productDto.getId()));
    }

    /**
     * Deletes a category.
     *
     * @param categoryDto the category to delete
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> deleteCategory(CategoryDto categoryDto) {
        return delete(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getCategory(), categoryDto.getId()));
    }

    /**
     * Creates a customization.
     *
     * @param customizationDto the customization to create
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> saveCustomization(CustomizationDto customizationDto) {
        return post(urlMenuProperties.getUrl().getCustomization(), Mono.just(customizationDto), CustomizationDto.class);
    }

    /**
     * Fetches a single customization.
     *
     * @param customizationId the customization id
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> getCustomizationById(Integer customizationId) {
        return get(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getCustomization(), customizationId));
    }

    /**
     * Updates an existing customization.
     *
     * @param customizationDto the customization to update
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateCustomization(CustomizationDto customizationDto) {
        return put(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getCustomization(), customizationDto.getId()),
                Mono.just(customizationDto), CustomizationDto.class);
    }

    /**
     * Deletes a customization.
     *
     * @param customizationDto the customization to delete
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> deleteCustomization(CustomizationDto customizationDto) {
        return delete(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getCustomization(), customizationDto.getId()));
    }

    private String productCustomizationUrl(Integer productId) {
        return FORMAT_STRING.formatted(urlMenuProperties.getUrl().getProduct(), productId)
                .concat("/customization");
    }

    /**
     * Lists the customizations linked to a product.
     *
     * @param productId the product id
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> getProductCustomizations(Integer productId) {
        return get(productCustomizationUrl(productId));
    }

    /**
     * Replaces the full set of customizations linked to a product.
     *
     * @param productId the product id
     * @param replaceDto the new customization links
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> saveProductCustomizations(Integer productId,
                                                           ProductCustomizationReplaceDto replaceDto) {
        return put(productCustomizationUrl(productId),
                Mono.just(replaceDto), ProductCustomizationReplaceDto.class);
    }

    /**
     * Updates a single product-customization link.
     *
     * @param productId the product id
     * @param linkId    the link id
     * @param configDto the new link configuration
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateProductCustomization(Integer productId, Integer linkId,
                                                            ProductCustomizationConfigDto configDto) {
        return put(FORMAT_STRING.formatted(productCustomizationUrl(productId), linkId),
                Mono.just(configDto), ProductCustomizationConfigDto.class);
    }

    /**
     * Removes a single product-customization link.
     *
     * @param productId the product id
     * @param linkId    the link id
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> deleteProductCustomization(Integer productId, Integer linkId) {
        return delete(FORMAT_STRING.formatted(productCustomizationUrl(productId), linkId));
    }

}
