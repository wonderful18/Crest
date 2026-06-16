package io.crest.dataset.dao.auto.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.crest.dataset.dao.auto.entity.CoreDatasetSyncTaskLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
// 定义当前业务对象的核心职责
public interface CoreDatasetSyncTaskLogMapper extends BaseMapper<CoreDatasetSyncTaskLog> {
}
