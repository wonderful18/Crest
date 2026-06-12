package io.crest.api.dataset.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class DatasetSyncTaskRequest implements Serializable {

    private String keyword;

    private String taskStatus;

    private String syncRate;

    private String updateType;
}
