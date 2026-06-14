package io.crest.api.template.request;

import io.crest.api.template.vo.VisualizationTemplateVO;
import lombok.Data;

import java.util.List;

@Data
public class TemplateManageBatchRequest {

    private String optType;

    private List<String> templateIds;

    private List<String> categories;

}
