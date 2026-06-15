package io.crest.visualization.server;

// 实现接口服务，衔接业务处理和返回结果
public enum FileType {

    /**
     * JPEG
     */
    JPEG("FFD8FF", "jpg"),

    /**
     * PNG
     */
    PNG("89504E47", "png"),

    /**
     * GIF
     */
    GIF("47494638", "gif");

    private String value = "";
    private String ext = "";

    FileType(String value) {
        this.value = value;
    }

    FileType(String value, String ext) {
        this(value);
        this.ext = ext;
    }

    // 执行当前服务方法并返回处理结果
    public String getExt() {
        return ext;
    }

    // 执行当前服务方法并返回处理结果
    public String getValue() {
        return value;
    }

}