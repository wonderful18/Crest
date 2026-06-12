package io.crest.api.dataset.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class DatasetSyncTaskPageVO implements Serializable {

    private Integer page;

    private Integer pageSize;

    private Long total;

    private List<DatasetSyncTaskDTO> records;
}
