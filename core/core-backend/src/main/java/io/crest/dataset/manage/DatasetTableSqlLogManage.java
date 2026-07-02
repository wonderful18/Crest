package io.crest.dataset.manage;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.crest.api.dataset.dto.SqlLogDTO;
import io.crest.dataset.dao.auto.entity.CoreDatasetTable;
import io.crest.dataset.dao.auto.entity.CoreDatasetTableSqlLog;
import io.crest.dataset.dao.auto.mapper.CoreDatasetTableMapper;
import io.crest.dataset.dao.auto.mapper.CoreDatasetTableSqlLogMapper;
import io.crest.exception.CrestException;
import io.crest.utils.BeanUtils;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@Transactional(rollbackFor = Exception.class)
public class DatasetTableSqlLogManage {
    @Resource
    private CoreDatasetTableSqlLogMapper coreDatasetTableSqlLogMapper;
    @Resource
    private CoreDatasetTableMapper coreDatasetTableMapper;
    @Resource
    private DatasetGroupManage datasetGroupManage;

    public void save(SqlLogDTO dto) {
        if (dto == null) {
            return;
        }
        requireTableAccess(dto.getTableId());
        CoreDatasetTableSqlLog coreDatasetTableSqlLog = new CoreDatasetTableSqlLog();
        BeanUtils.copyBean(coreDatasetTableSqlLog, dto);
        if (ObjectUtils.isEmpty(coreDatasetTableSqlLog.getId())) {
            coreDatasetTableSqlLog.setId(UUID.randomUUID().toString());
            coreDatasetTableSqlLogMapper.insert(coreDatasetTableSqlLog);
        } else {
            coreDatasetTableSqlLogMapper.updateById(coreDatasetTableSqlLog);
        }
    }

    public List<SqlLogDTO> listByTableId(SqlLogDTO dto) {
        if (dto == null || ObjectUtils.isEmpty(dto.getTableId())) {
            return null;
        }
        requireTableAccess(dto.getTableId());
        QueryWrapper<CoreDatasetTableSqlLog> wrapper = new QueryWrapper<>();
        wrapper.eq("table_id", dto.getTableId());
        List<CoreDatasetTableSqlLog> coreDatasetTableSqlLogs = coreDatasetTableSqlLogMapper.selectList(wrapper);
        return coreDatasetTableSqlLogs.stream().map(ele -> {
            SqlLogDTO s = new SqlLogDTO();
            BeanUtils.copyBean(s, ele);
            return s;
        }).collect(Collectors.toList());
    }

    public void tableRemoval(String id) {
        requireTableAccess(id);
        QueryWrapper<CoreDatasetTableSqlLog> wrapper = new QueryWrapper<>();
        wrapper.eq("table_id", id);
        coreDatasetTableSqlLogMapper.delete(wrapper);
    }

    private void requireTableAccess(String tableId) {
        Long id = parseTableId(tableId);
        CoreDatasetTable table = coreDatasetTableMapper.selectById(id);
        if (table == null || table.getDatasetGroupId() == null) {
            CrestException.throwException("数据集表不存在或无访问权限");
        }
        datasetGroupManage.requireDatasetAccess(table.getDatasetGroupId());
    }

    private Long parseTableId(String tableId) {
        String trimmed = StringUtils.trimToNull(tableId);
        if (trimmed == null) {
            CrestException.throwException("数据集表ID不能为空");
        }
        try {
            return Long.valueOf(trimmed);
        } catch (NumberFormatException e) {
            CrestException.throwException("数据集表ID不合法");
            return null;
        }
    }
}
