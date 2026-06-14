package io.crest.api.dataset.engine;

import lombok.Data;

@Data
public class SQLFunctionDTO {
    private String name;
    private String func;
    private int type;
    private String desc;
    private boolean isCustom;
}
