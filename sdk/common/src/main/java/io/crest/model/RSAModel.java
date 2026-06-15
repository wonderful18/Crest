package io.crest.model;

import lombok.Data;

@Data
// 定义当前业务对象的核心职责
public class RSAModel {

    private String privateKey;

    private String publicKey;

    private String aesKey;
}
