package io.crest.model;

import io.crest.constant.LogST;
import lombok.Data;

import java.io.Serializable;

@Data
// 定义当前业务对象的核心职责
public class LogItemModel implements Serializable {

    private Long id;

    private String name;

    private LogST st;
}
