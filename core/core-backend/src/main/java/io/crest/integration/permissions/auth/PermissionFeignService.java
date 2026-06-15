package io.crest.integration.permissions.auth;

import io.crest.feign.CrestFeign;
import io.crest.api.permissions.auth.api.AuthApi;


@CrestFeign(value = "permissions", path = "/auth")
// 定义当前业务对象的核心职责
public interface PermissionFeignService extends AuthApi {

}
