package io.crest.api.template.vo;

import lombok.Data;

@Data
public class TemplateCategoryVO {
    private Integer id;

    private String name;

    private String slug;

    private Integer priority;
}
