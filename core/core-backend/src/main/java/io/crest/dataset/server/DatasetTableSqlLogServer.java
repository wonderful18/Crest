package io.crest.dataset.server;

import io.crest.api.dataset.DatasetTableSqlLogApi;
import io.crest.api.dataset.dto.SqlLogDTO;
import io.crest.dataset.manage.DatasetTableSqlLogManage;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("dataset-table-sql-log")
public class DatasetTableSqlLogServer implements DatasetTableSqlLogApi {
    @Resource
    private DatasetTableSqlLogManage datasetTableSqlLogManage;

    @Override
    public void save(@RequestBody SqlLogDTO sqlLogDTO) {
        datasetTableSqlLogManage.save(sqlLogDTO);
    }

    @Override
    public List<SqlLogDTO> listByTableId(@RequestBody SqlLogDTO sqlLogDTO) {
        return datasetTableSqlLogManage.listByTableId(sqlLogDTO);
    }

    @Override
    public void tableRemoval(String id) {
        datasetTableSqlLogManage.tableRemoval(id);
    }
}
