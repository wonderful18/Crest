package io.crest.dataset.server;

import io.crest.api.dataset.DatasetSyncApi;
import io.crest.api.dataset.dto.DatasetSyncLogDTO;
import io.crest.api.dataset.dto.DatasetSyncTaskPageVO;
import io.crest.api.dataset.dto.DatasetSyncTaskRequest;
import io.crest.api.dataset.dto.DatasetSyncTaskDTO;
import io.crest.dataset.sync.DatasetSyncManage;
import io.crest.dataset.sync.DatasetSyncTaskManage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/dataset-sync")
public class DatasetSyncServer implements DatasetSyncApi {

    @Resource
    private DatasetSyncTaskManage taskManage;
    @Resource
    private DatasetSyncManage syncManage;

    @Override
    public DatasetSyncTaskPageVO page(Integer page, Integer pageSize, DatasetSyncTaskRequest request) {
        return taskManage.page(page, pageSize, request);
    }

    @Override
    public List<DatasetSyncTaskDTO> dependencies(Long visualizationId) {
        return taskManage.dependencies(visualizationId);
    }

    @Override
    public DatasetSyncTaskDTO save(DatasetSyncTaskDTO task) {
        return taskManage.save(task);
    }

    @Override
    public DatasetSyncTaskDTO task(Long datasetGroupId) {
        return taskManage.task(datasetGroupId);
    }

    @Override
    public DatasetSyncTaskDTO execute(Long datasetGroupId) throws Exception {
        return syncManage.executeNow(datasetGroupId);
    }

    @Override
    public void stop(Long datasetGroupId) {
        taskManage.stop(datasetGroupId);
    }

    @Override
    public DatasetSyncTaskDTO pause(Long datasetGroupId) {
        return taskManage.pause(datasetGroupId);
    }

    @Override
    public DatasetSyncTaskDTO resume(Long datasetGroupId) {
        return taskManage.resume(datasetGroupId);
    }

    @Override
    public DatasetSyncTaskDTO retry(Long datasetGroupId) throws Exception {
        return syncManage.executeNow(datasetGroupId);
    }

    @Override
    public List<DatasetSyncLogDTO> logs(Long datasetGroupId) {
        return taskManage.logs(datasetGroupId);
    }
}
