package com.yang.detail;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Cocoicobird
 * @version 1.0
 */
@Data
public class ApiVersionDetail {
    /**
     * key 为微服务名称
     * value 为对应方法和 url
     */
    private Map<String, Map<String, String>> noVersion;
    private Map<String, Map<String, String>> missingUrl;

    public ApiVersionDetail() {
        this.noVersion = new HashMap<>();
        this.missingUrl = new HashMap<>();
    }
}
