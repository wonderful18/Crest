package io.crest.api.dataset.dto;

import lombok.Data;

@Data
public class DataSetExportRequest extends DatasetNodeDTO {
    private String filename;
    private String expressionTree;
    private boolean embeddedExport;
}
