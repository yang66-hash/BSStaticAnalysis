package com.yang.item;

import lombok.Data;

/**
 * @author Cocoicobird
 * @version 1.0
 * @description 微服务实体类数量
 */
@Data
public class ServiceCutItem {
    private String microserviceName;
    private Integer entityCount;
}
