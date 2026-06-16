package io.crest.datasource.dao.auto.mapper;

import io.crest.datasource.dao.auto.entity.QrtzSchedulerState;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * Quartz 调度器状态 Mapper。
 */
@Mapper
public interface QrtzSchedulerStateMapper extends BaseMapper<QrtzSchedulerState> {

}
