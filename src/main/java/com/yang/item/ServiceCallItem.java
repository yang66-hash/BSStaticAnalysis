package com.yang.item;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Cocoicobird
 * @version 1.0
 * @description 某一个微服务的调用情况，包括其调用的服务及其被哪些服务调用
 */
@Data
public class ServiceCallItem {
    private String microservice;
    private Set<String> callServices; // 调用的服务
    private Set<String> calledServices; // 被哪些服务调用
    private boolean isESBUsage;

    public ServiceCallItem() {
        this.callServices = new LinkedHashSet<>();
        this.calledServices = new LinkedHashSet<>();
    }

    public ServiceCallItem(String microservice) {
        this.microservice = microservice;
        this.callServices = new LinkedHashSet<>();
        this.calledServices = new LinkedHashSet<>();
    }
}
