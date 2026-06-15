package io.crest.constant;

// 定义当前业务支持的枚举取值
public enum AuthEnum {
    READ(1), USER(2), EXPORT(4), EXPORT_VIEW(5), EXPORT_DETAIL(6), MANAGE(7), AUTH(9);
    private Integer weight;

    // 执行当前服务方法并返回处理结果
    public Integer getWeight() {
        return weight;
    }

    // 执行当前服务方法并返回处理结果
    public void setWeight(Integer weight) {
        this.weight = weight;
    }

    AuthEnum(Integer weight) {
        this.weight = weight;
    }

    AuthEnum() {
    }
}
