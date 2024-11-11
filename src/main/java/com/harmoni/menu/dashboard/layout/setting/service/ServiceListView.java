package com.harmoni.menu.dashboard.layout.setting.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.menu.product.ProductForm;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@UIScope
@PreserveOnRefresh
@Route(value = "service", layout = MainLayout.class)
@PageTitle("Service | POSHarmoni")
@Component
@Slf4j
public class ServiceListView extends VerticalLayout {

    Registration broadcasterRegistration;

    private final TreeGrid<ServiceTreeItem> serviceTreeGrid = new TreeGrid<>(ServiceTreeItem.class);
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    private final ComboBox<TierDto> tierDtoComboBox = new ComboBox<>();
    private final ComboBox<BrandDto> brandDtoComboBox = new ComboBox<>();
    private ProductForm productForm;
    private transient TierDto tierDto;
    private transient BrandDto brandDto;
    private UI ui;

    public ServiceListView(@Autowired AsyncRestClientMenuService asyncRestClientMenuService,
                           @Autowired RestClientMenuService restClientMenuService,
                           @Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService) {

        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.restClientMenuService = restClientMenuService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;

        addClassName("list-view");
        setSizeFull();

        configureGrid();
        configureForm();

        add(getToolbar(), getContent());

        closeEditor();
    }

    private void configureGrid() {
        serviceTreeGrid.setSizeFull();
        serviceTreeGrid.removeAllColumns();
        serviceTreeGrid.addHierarchyColumn(ServiceTreeItem::getName).setHeader("Store Name");
        serviceTreeGrid.addColumn(ServiceTreeItem::getServiceName).setHeader("Service Name");
        serviceTreeGrid.addColumn(ServiceTreeItem::getSubServiceName).setHeader("Sub Service Name");
        serviceTreeGrid.addColumn(ServiceTreeItem::isActive).setHeader("Active");

        serviceTreeGrid.addCollapseListener(event -> event.getItems().forEach(serviceTreeItem ->
                log.debug("item collapse {}", serviceTreeItem)));

        serviceTreeGrid.addExpandListener(event -> {
            if (event.isFromClient()) {
                event.getItems().forEach(productTreeItem -> {
                    log.debug("item expand {}", productTreeItem);
                    List<Integer> skuIds = new ArrayList<>();
                });
            }
        });

        serviceTreeGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));

    }

    private void configureForm() {
        productForm = new ProductForm(this.asyncRestClientMenuService);
        productForm.setWidth("25em");
    }


    private HorizontalLayout getToolbar() {

        brandDtoComboBox.setLabel("Brand");
        brandDtoComboBox.setItems(new ArrayList<>());
        brandDtoComboBox.setItemLabelGenerator(BrandDto::getName);
        brandDtoComboBox.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.isFromClient()) {
                brandDto = valueChangeEvent.getValue();
                //fetchCategories(brandDto.getId());
            }
        });

        Button addBrandButton = new Button("Add Service");
        addBrandButton.addClickListener(buttonClickEvent -> addService());
        HorizontalLayout toolbar = new HorizontalLayout(brandDtoComboBox, addBrandButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(serviceTreeGrid, productForm);
        content.setFlexGrow(1, serviceTreeGrid);
        content.setFlexGrow(1, productForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });

        this.fetchBrands();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void fetchBrands() {

        restClientMenuService.getAllBrand()
                .subscribe(restAPIResponse -> {
                    if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {

                        final List<BrandDto> brands = ObjectUtil.convertObjectToObject(restAPIResponse.getData(),
                                new TypeReference<List<BrandDto>>() {
                                });

                        brandDto = brands.getFirst();
                        if (!ObjectUtils.isEmpty(brands)) {
                            ui.access(() -> {
                                brandDtoComboBox.setItems(brands);
                                brandDtoComboBox.setValue(brandDto);
                            });

                            //fetchCategories(brandDto.getId());
                            fetchTier(brandDto.getId());

                        }
                    }
                });
    }

    private void addService() {
        serviceTreeGrid.asSingleSelect().clear();
        editProduct(new ProductDto(), FormAction.CREATE);
    }

    public void editProduct(ProductDto productDto, FormAction formAction) {
        if (productDto == null) {
            closeEditor();
        } else {

        }
    }

    private void closeEditor() {
        productForm.setVisible(false);
        removeClassName("editing");
    }

    private void fetchServices() {

    }

    private void fetchTier(Integer brandId) {
    }

    private void fetchAllStore(Integer brandId) {
        asyncRestClientOrganizationService.getAllStoreAsync(result -> {
            TreeData<ServiceTreeItem> serviceTreeItemTreeData = new TreeData<>();
//            result.forEach(storeDto -> {
//                ProductTreeItem productTreeItem =  ProductTreeItem.builder()
//                        .id("%s|%d".formatted(ProductItemType.PRODUCT, productDto.getId()))
//                        .name(productDto.getName())
//                        .productId(productDto.getId())
//                        .categoryId(productDto.getCategoryId())
//                        .categoryName(productDto.getCategoryDto().getName())
//                        .productItemType(ProductItemType.PRODUCT)
//                        .skus(productDto.getSkuDtos())
//                        .build();
////                serviceTreeItemTreeData.addItems(null, productTreeItem);
////                serviceTreeItemTreeData.addItems(productTreeItem, getSkus(productDto));
//            });

//            ui.access(()-> productDtoGrid.setTreeData(productDtoTreeData));

        });
    }

}
