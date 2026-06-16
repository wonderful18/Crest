package io.crest.constant;


// 定义当前业务支持的枚举取值
public enum BusiResourceEnum {
    PANEL(1), SCREEN(2), DATASET(3), DATASOURCE(4), DATA_FILLING(8);

    private int flag;

    // 执行当前服务方法并返回处理结果
    public int getFlag() {
        return flag;
    }

    // 执行当前服务方法并返回处理结果
    public void setFlag(int flag) {
        this.flag = flag;
    }

    BusiResourceEnum(int flag) {
        this.flag = flag;
    }
}
