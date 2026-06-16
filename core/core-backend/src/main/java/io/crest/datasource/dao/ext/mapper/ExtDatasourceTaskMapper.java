package io.crest.datasource.dao.ext.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.crest.datasource.dto.CoreDatasourceTaskDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
// 定义当前业务对象的核心职责
public interface ExtDatasourceTaskMapper {


    @Select(
            """     
                    SELECT QRTZ_TRIGGERS.* 
                    FROM QRTZ_TRIGGERS
                     ${ew.customSqlSegment}
                    """
    )
    @Results(
            id = "taskWithTriggers",
            value = {
                    @Result(property = "id", column = "id"),
                    @Result(property = "datasourceName", column = "datasource_name"),
                    @Result(property = "dsId", column = "ds_id"),
                    @Result(property = "nextExecTime", column = "NEXT_FIRE_TIME")
            }
    )
    List<CoreDatasourceTaskDTO> taskWithTriggers(@Param("ew") QueryWrapper queryWrapper);


}
