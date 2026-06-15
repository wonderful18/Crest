package io.crest.integration.permissions.user;

import io.crest.api.permissions.user.api.UserApi;
import io.crest.feign.CrestFeign;

@CrestFeign(value = "permissions", path = "/user")
// 定义当前业务对象的核心职责
public interface UserFeignService extends UserApi {
}
