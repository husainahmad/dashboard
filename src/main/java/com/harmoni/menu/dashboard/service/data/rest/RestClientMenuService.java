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

@RequiredArgsConstructor
@Service
@Slf4j
public class RestClientMenuService extends RestClientService implements Serializable {

    private final transient MenuProperties urlMenuProperties;
    private static final String FORMAT_STRING = "%s/%d";

    public Mono<RestAPIResponse> createCategory(CategoryDto categoryDto) {
        return post(urlMenuProperties.getUrl().getCategory(), Mono.just(categoryDto), CategoryDto.class);
    }

    public Mono<RestAPIResponse> getAllCategoryByBrand(Integer brandId) {
        return get(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getCategories().getBrand(), brandId));
    }

    public Mono<RestAPIResponse> getAllTierByBrand(Integer brandId, String type) {
        return get("%s/brand/%d/type/%s".formatted(urlMenuProperties.getUrl().getTier(), brandId, type));
    }

    public Mono<RestAPIResponse> getAllBrand() {
        return get(urlMenuProperties.getUrl().getBrand());
    }

    public Mono<RestAPIResponse> getProduct(Integer productId) {
        return get(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getProduct(), productId));
    }

    public Mono<RestAPIResponse> saveProduct(ProductDto productDto) {
        return post(urlMenuProperties.getUrl().getProduct(),
                Mono.just(productDto),  ProductDto.class);
    }

    public Mono<RestAPIResponse> updateProduct(ProductDto productDto) {
        return put(urlMenuProperties.getUrl().getProduct(),
                Mono.just(productDto),  ProductDto.class);
    }

    public Mono<RestAPIResponse> uploadProduct(ImageDto imageDto) throws IOException {
        File file = ImageUtil.convertImageDtoToFile(imageDto);
        return upload(urlMenuProperties.getUrl().getProducts().getImages().getUpload(),
                file);
    }

    public Mono<RestAPIResponse> uploadUpdatedProduct(Integer productId, ImageDto imageDto) throws IOException {
        File file = ImageUtil.convertImageDtoToFile(imageDto);
        return uploadUpdate(urlMenuProperties.getUrl().getProducts().getImages().getUploadUpdate().formatted(productId), file);
    }

    public Mono<RestAPIResponse> deleteProduct(ProductDto productDto) {
        return delete(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getProduct(), productDto.getId()));
    }

    public Mono<RestAPIResponse> deleteCategory(CategoryDto categoryDto) {
        return delete(FORMAT_STRING.formatted(urlMenuProperties.getUrl().getCategory(), categoryDto.getId()));
    }

}
