package io.crest.model;

import java.util.List;

// 定义当前业务对象的核心职责
public interface ITreeBase<T> {

    String getNodeType();

    Long getId();

    void setId(Long id);

    Long getPid();

    void setPid(Long pid);

    List<T> getChildren();

    void setChildren(List<T> children);
}
