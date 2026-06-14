package io.crest.extensions.datasource.dto;

import lombok.Data;

@Data
public class DsTypeDTO {
    private String type;
    private String name;

    private String catalog;
    private String prefix;
    private String suffix;
}
