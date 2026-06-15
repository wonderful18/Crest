package io.crest.model;

import java.io.Serializable;

// 定义当前业务对象的核心职责
public interface TreeBaseModel<T> extends Serializable {

    Long getId();

    Long getPid();

    String getName();

    void setName(String name);
}
