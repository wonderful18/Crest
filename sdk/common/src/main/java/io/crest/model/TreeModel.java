package io.crest.model;

import lombok.Data;

import java.util.List;

@Data
// 定义当前业务对象的核心职责
public class TreeModel<T>{

    private TreeBaseModel<T> data;

    private List<TreeModel> children;

    public TreeModel(TreeBaseModel<T> data) {
        this.data = data;
    }

    // 执行当前服务方法并返回处理结果
    public Long getId() {
        return data.getId();
    }

    // 执行当前服务方法并返回处理结果
    public Long getPid() {
        return data.getPid();
    }

    // 执行当前服务方法并返回处理结果
    public String getName() {
        return data.getName();
    }
}
