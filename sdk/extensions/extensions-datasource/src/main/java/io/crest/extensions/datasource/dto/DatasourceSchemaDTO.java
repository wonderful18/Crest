package io.crest.extensions.datasource.dto;

import lombok.Data;

@Data
public class DatasourceSchemaDTO extends DatasourceDTO {
    private String schemaAlias;
}
