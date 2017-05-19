package com.sedis.util;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.ValueNode;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.text.SimpleDateFormat;

public abstract class JsonUtil {

    private static ObjectMapper objectMapper = null;

    static {
        objectMapper = new ObjectMapper();
        //序列化对象时：空对象不会出现在json中
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_EMPTY);
        //反序列化时：忽略未知属性（默认为true，会抛出异常）
        objectMapper.configure(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationConfig.Feature.WRITE_DATE_KEYS_AS_TIMESTAMPS, false);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        objectMapper.getSerializationConfig().setDateFormat(dateFormat);
        objectMapper.getDeserializationConfig().setDateFormat(dateFormat);
    }

    /**
     * 在JSON格式的字符串中获取值，支持路径、数组
     */
    public static String searchValue(String jsonContent, String name) {
        try {
            return searchValue(objectMapper.readTree(jsonContent), name);
        } catch (IOException e) {
            throw new RuntimeException("解析JSON串异常,", e);
        }
    }

    private static String searchValue(JsonNode jsonNode, String name) {
        if (jsonNode == null) {
            return null;
        }
        if (jsonNode instanceof ValueNode) {
            return jsonNode.asText();
        } else if (jsonNode instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) jsonNode;
            for (JsonNode node : arrayNode) {
                return searchValue(node, name);
            }
        } else if (jsonNode instanceof ObjectNode) {
            String currentName = name;
            String subName = null;
            if (name.indexOf(".") > -1) {
                currentName = currentName.substring(0, name.indexOf("."));
                subName = name.substring(currentName.length() + 1);
            }
            return searchValue(jsonNode.get(currentName), subName);
        }
        return null;
    }

    /**
     * 将JavaBean实例转换成JSON格式的字符串
     */
    public static String beanToJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (IOException e) {
            throw new RuntimeException("将对象[" + object.getClass().getName() + "]转换成JSON串异常,", e);
        }
    }

    /**
     * json字符串转换成Array
     */
    public static <T extends Object> T jsonToBean(String jsonContent, JavaType javaType) {
        try {
            return (T) objectMapper.readValue(jsonContent, javaType);
        } catch (IOException e) {
            throw new RuntimeException("JSON转换成Bean异常", e);
        }
    }

    /**
     * json字符串转换成Array
     */
    public static <T extends Object> T jsonToBean(String jsonContent, Class<T> clazz) {
        try {
            return objectMapper.readValue(jsonContent, clazz);
        } catch (IOException e) {
            throw new RuntimeException("JSON转换成Bean异常", e);
        }
    }

    /**
     * json字符串转换成Array
     */
    public static <T extends Object> T jsonToBean(String jsonContent, TypeReference typeReference) {
        try {
            return (T) objectMapper.readValue(jsonContent, typeReference);
        } catch (IOException e) {
            throw new RuntimeException("JSON转换成Bean异常", e);
        }
    }

}