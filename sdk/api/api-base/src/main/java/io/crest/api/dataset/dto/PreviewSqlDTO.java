package io.crest.api.dataset.dto;

import lombok.Data;

@Data
public class PreviewSqlDTO {
    private String tableId;
    private String sql;
    private Long datasourceId;
    private String sqlVariableDetails;
    private Boolean isCross;
}
