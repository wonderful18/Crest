package io.crest.asset.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class DataAssetPageVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 3257039053618385840L;

    private long total;
    private int page;
    private int pageSize;
    private List<DataAssetVO> records = new ArrayList<>();
}
