package io.crest.constant;

import java.util.Arrays;

// 定义当前业务支持的枚举取值
public enum ReportTaskEnum {

    WAIT(0), SEND(1), STOP(2), FINISH(3);

    private Integer flag;

    // 执行当前服务方法并返回处理结果
    public Integer getFlag() {
        return flag;
    }

    // 执行当前服务方法并返回处理结果
    public void setFlag(Integer flag) {
        this.flag = flag;
    }

    ReportTaskEnum(Integer flag) {
        this.flag = flag;
    }

    ReportTaskEnum() {
    }

    // 执行当前服务方法并返回处理结果
    public static ReportTaskEnum fromValue(Integer flag) {
        return Arrays.stream(values()).filter(v -> v.flag.equals(flag)).findFirst().get();
    }
}
