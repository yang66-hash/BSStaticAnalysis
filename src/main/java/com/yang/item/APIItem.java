package com.yang.item;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Cocoicobird
 * @version 1.0
 * @description 存储 .java 文件的 url
 * url1 为类上注解配置的 url
 * url2 为方法上注解配置的 url
 * maxParamNum 存储当前.java文件中所有API参数的的最大数值
 */
@Data
public class APIItem {
    private String url1;
    private Map<String, String> url2;
    private Integer maxParaNum;

    public APIItem() {
        this.url2 = new HashMap<>();
        this.maxParaNum = 0;
    }
}
