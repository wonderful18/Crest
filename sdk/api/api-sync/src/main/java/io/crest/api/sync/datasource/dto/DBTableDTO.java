package io.crest.api.sync.datasource.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DBTableDTO {
    private String datasourceId;
    private String name;
    private String remark;
    private boolean enableCheck;
    private String datasetPath;
}
