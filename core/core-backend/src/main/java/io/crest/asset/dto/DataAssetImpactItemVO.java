package io.crest.asset.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class DataAssetImpactItemVO implements Serializable {
    @Serial
    private static final long serialVersionUID = 4702790418212923136L;

    private String assetType;
    private String assetTypeLabel;
    private String assetId;
    private String name;
    private String relation;
    private Long updateTime;
}
