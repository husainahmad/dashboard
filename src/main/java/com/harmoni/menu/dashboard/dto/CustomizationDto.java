package com.harmoni.menu.dashboard.dto;

import com.harmoni.menu.dashboard.layout.enums.SelectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizationDto {

    private Integer id;
    private String name;
    private String description;
    private SelectionType selectionType;
    private List<CustomizationOptionDto> customizationOptions;

}