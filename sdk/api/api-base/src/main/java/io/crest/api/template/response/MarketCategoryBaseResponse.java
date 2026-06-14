package io.crest.api.template.response;

import io.crest.api.template.vo.TemplateCategoryVO;
import lombok.Data;

import java.util.List;

@Data
public class MarketCategoryBaseResponse {
    private List<TemplateCategoryVO> data;
}
