package io.crest.api.dataset.union;

import lombok.Data;

import java.io.Serializable;

@Data
public class DatasetTableInfoDTO implements Serializable {
    private String table;
    private String sql;
}
