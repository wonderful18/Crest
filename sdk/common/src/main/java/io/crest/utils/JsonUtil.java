package io.crest.utils;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.List;

/**
 * Jackson JSON 序列化和反序列化工具。
 */
public class JsonUtil {

    /**
     * 全局共享的对象映射器。
     */
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        // 配置更大的 StreamReadConstraints 限制
        objectMapper.getFactory().setStreamReadConstraints(
                StreamReadConstraints.builder()
                        .maxStringLength(50000000)
                        .build()
        );
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * 将 JSON 字符串解析为目标类型实例。
     */
    public static <T> T parse(String json, Class<T> classOfT) {
        if (json == null) return null;
        T t = null;
        try {
            t = objectMapper.readValue(json, new TypeReference<T>() {
            });
        } catch (JsonProcessingException e) {
            LogUtil.error(e.getMessage(), e);
        }
        return t;
    }

    /**
     * 将 JSON 字符串按指定 Class 解析为对象。
     */
    public static <T> T parseObject(String json, Class<T> classOfT) {
        if (json == null) return null;
        T t = null;
        try {
            t = objectMapper.readValue(json, classOfT);
        } catch (JsonProcessingException e) {
            LogUtil.error(e.getMessage(), e);
        }
        return t;
    }

    /**
     * 将 JSON 字符串按类型引用解析为对象。
     */
    public static <T> T parseObject(String json, TypeReference<T> typeReference) {
        if (json == null) return null;
        T t = null;
        try {
            t = objectMapper.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            LogUtil.error(e.getMessage(), e);
        }
        return t;
    }

    /**
     * 将 JSON 字符串解析为列表，空输入返回空列表。
     */
    public static <T> List<T> parseList(String json, TypeReference<List<T>> classOfT) {
        if (ObjectUtils.isEmpty(json)) return Collections.emptyList();
        List<T> t = null;
        try {
            t = objectMapper.readValue(json, classOfT);
        } catch (JsonProcessingException e) {
            LogUtil.error(e.getMessage(), e);
        }
        return t;
    }

    /**
     * 将对象序列化为 JSON 字符串。
     */
    public static Object toJSONString(Object o) {

        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            LogUtil.error(e.getMessage(), e);
            return null;
        }
    }

}
