package io.crest.commons.utils;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MybatisInterceptorConfig {
    private String modelName;
    private String attrName;
    private String attrNameForList;
    private String interceptorClass;
    private String interceptorMethod;
    private String undoClass;
    private String undoMethod;

    public MybatisInterceptorConfig() {
    }

    /**
     * 使用默认的字段加解密处理器配置实体字段。
     *
     * @param modelClass 需要处理的实体类型
     * @param attrName 需要加解密的字段名
     */
    public MybatisInterceptorConfig(Class<?> modelClass, String attrName) {
        this.modelName = modelClass.getName();
        this.attrName = attrName;
        this.interceptorClass = EncryptUtils.class.getName();
        this.interceptorMethod = "aesEncrypt";
        this.undoClass = EncryptUtils.class.getName();
        this.undoMethod = "aesDecrypt";
    }

    public MybatisInterceptorConfig(Class<?> modelClass, String attrName, Class<?> interceptorClass, String interceptorMethod, String undoMethod) {
        this.modelName = modelClass.getName();
        this.attrName = attrName;
        this.interceptorClass = interceptorClass.getName();
        this.interceptorMethod = interceptorMethod;
        this.undoClass = interceptorClass.getName();
        this.undoMethod = undoMethod;
    }

}
