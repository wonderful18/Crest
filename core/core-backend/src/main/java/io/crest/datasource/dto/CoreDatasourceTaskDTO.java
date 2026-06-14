package io.crest.datasource.dto;

import io.crest.datasource.dao.auto.entity.CoreDatasourceTask;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoreDatasourceTaskDTO extends CoreDatasourceTask {
    private String datasourceName;
    private Long nextExecTime;
    private String taskStatus;
    private String msg;
    private String privileges;
}
