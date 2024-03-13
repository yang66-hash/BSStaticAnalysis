package com.yang.component;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Cocoicobird
 * @version 1.0
 * @description 配置文件实体类，存储每个微服务的配置文件项
 */
@Data
@AllArgsConstructor
public class Configuration {
    private String microserviceName;

    private Map<String, String> items;

    public Configuration() {
        this.items = new HashMap<>();
    }

    public String get(String key) {
        return this.items.get(key);
    }

    public void add(String key, String value) {
        this.items.put(key, value);
    }
}
