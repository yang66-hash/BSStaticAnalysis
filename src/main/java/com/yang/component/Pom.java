package com.yang.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.maven.model.Model;

/**
 * @author Cocoicobird
 * @version 1.0
 * @description Pom文件实体类，存储每个微服务的依赖
 */
@Data
@AllArgsConstructor
public class Pom {
    private String microserviceName;

    private Model mavenModel;

    public Pom() { }
}
