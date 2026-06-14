package io.crest.api.template.vo;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class MarketMetasVO {
    private String theme_repo;

    public MarketMetasVO(String theme_repo) {
        this.theme_repo = theme_repo;
    }
}
