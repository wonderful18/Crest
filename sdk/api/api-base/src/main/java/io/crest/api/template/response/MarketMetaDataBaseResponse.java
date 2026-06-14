package io.crest.api.template.response;

import io.crest.api.template.vo.MarketMetaDataVO;
import lombok.Data;

import java.util.List;

@Data
public class MarketMetaDataBaseResponse {

    private List<MarketMetaDataVO> platformVersion;

    private List<MarketMetaDataVO> templateTypes;

    private List<MarketMetaDataVO> labels;
}
