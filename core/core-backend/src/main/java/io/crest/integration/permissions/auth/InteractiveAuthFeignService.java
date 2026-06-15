package io.crest.integration.permissions.auth;

import io.crest.api.permissions.auth.api.InteractiveAuthApi;
import io.crest.feign.CrestFeign;

@CrestFeign(value = "permissions", path = "/interactive")
// 定义当前业务对象的核心职责
public interface InteractiveAuthFeignService extends InteractiveAuthApi {
}
