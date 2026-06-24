package io.crest.datasource.dao.auto.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

/**
 * 数据源完成页配置实体。
 */
@TableName("core_datasource_finish_page")
public class CoreDsFinishPage implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    // 执行当前服务方法并返回处理结果
    public Long getId() {
        return id;
    }

    // 执行当前服务方法并返回处理结果
    public void setId(Long id) {
        this.id = id;
    }

    @Override
    // 执行当前服务方法并返回处理结果
    public String toString() {
        return "CoreDsFinishPage{" +
        "id = " + id +
        "}";
    }
}
