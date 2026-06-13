package io.crest.datasource.dao.auto.mapper;

import io.crest.datasource.dao.auto.entity.CoreDatasourceTask;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据源同步任务 Mapper。
 */
@Mapper
public interface CoreDatasourceTaskMapper extends BaseMapper<CoreDatasourceTask> {

}
