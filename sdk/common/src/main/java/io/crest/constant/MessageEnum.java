package io.crest.constant;

import java.util.Arrays;

// 定义当前业务支持的枚举取值
public enum MessageEnum {

    INNER(0), EMAIL(1), WECOM(2), DINGTALK(3), LARK(4), LARKSUITE(5), LARKGROUP(6), WEBHOOK(7), LARKSUITEGROUP(8), DINGTALKGROUP(9);
    private Integer flag;

    // 执行当前服务方法并返回处理结果
    public Integer getFlag() {
        return flag;
    }

    // 执行当前服务方法并返回处理结果
    public void setFlag(Integer flag) {
        this.flag = flag;
    }

    MessageEnum(Integer flag) {
        this.flag = flag;
    }

    MessageEnum() {
    }

    // 执行当前服务方法并返回处理结果
    public static MessageEnum fromValue(Integer flag) {
        return Arrays.stream(values()).filter(v -> v.flag.equals(flag)).findFirst().get();
    }
}
