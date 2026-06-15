package io.crest.system.bo;

import lombok.Data;

import java.io.Serializable;

@Data
// 定义当前业务对象的核心职责
public class SysParameterBO implements Serializable {

    private String key;

    private String val;

    private String type;

    private String sort;
}
