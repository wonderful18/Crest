package io.crest.substitute.permissions.user.model;

import lombok.Data;

@Data
// 定义当前业务对象的核心职责
public class SsoUserProfile {
    private String externalId;
    private String account;
    private String name;
    private String email;
}
