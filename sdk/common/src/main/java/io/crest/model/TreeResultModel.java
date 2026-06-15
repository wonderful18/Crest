package io.crest.model;

import java.util.List;

// 定义当前业务对象的核心职责
public interface TreeResultModel<T> {

    void setChildren(List<T> children);
}
