package io.crest.extensions.view.dto;

import lombok.Data;

import java.util.List;

@Data
public class ChartDrillRequest {
    private List<ChartDimensionDTO> dimensionList;
}
