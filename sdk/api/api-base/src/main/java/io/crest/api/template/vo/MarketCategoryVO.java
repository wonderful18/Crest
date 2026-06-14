package io.crest.api.template.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MarketCategoryVO {
    private String id;
    private String name;
    private String slug;

    public MarketCategoryVO(String name) {
        this.name = name;
    }
}
