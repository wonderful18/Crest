package io.crest.utils;

// 提供当前模块复用的工具能力
public class CommunityUtils {


    private static final ThreadLocal<String> COMMUNITY_INFO = new ThreadLocal<>();

    // 执行当前服务方法并返回处理结果
    public static void setInfo(String info) {
        COMMUNITY_INFO.set(info);
    }

    // 执行当前服务方法并返回处理结果
    public static String getInfo() {
        return COMMUNITY_INFO.get();
    }

    // 执行当前服务方法并返回处理结果
    public static void removeInfo() {
        COMMUNITY_INFO.remove();
    }

}
