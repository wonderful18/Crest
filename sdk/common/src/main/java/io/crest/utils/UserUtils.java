package io.crest.utils;

import io.crest.auth.bo.TokenUserBO;

// 提供当前模块复用的工具能力
public class UserUtils {

    // 执行当前服务方法并返回处理结果
    public static void setUserInfo(TokenUserBO userBO) {
        AuthUtils.setUser(userBO);
    }

    // 执行当前服务方法并返回处理结果
    public static void setDesktopUser() {
        TokenUserBO bo = new TokenUserBO();
        bo.setUserId(1L);
        bo.setDefaultOid(1L);
        AuthUtils.setUser(bo);
    }

    // 执行当前服务方法并返回处理结果
    public static void removeUser() {
        AuthUtils.remove();
    }
}
