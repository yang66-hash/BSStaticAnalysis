package com.yang.item;

import lombok.Data;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Cocoicobird
 * @version 1.0
 * @description 类及其直接引用
 */
@Data
public class CyclicReferenceItem {
    /**
     * 超类
     */
    private String superClass;
    private String microserviceName;
    /**
     * 出现在超类中的子类
     */
    private Set<String> subClasses;
    private int subclassNum;

    public CyclicReferenceItem(){
        this.subClasses = new LinkedHashSet<>();
    }

    public CyclicReferenceItem(String microserviceName, String superClass){
        this.microserviceName = microserviceName;
        this.superClass = superClass;
        this.subClasses = new LinkedHashSet<>();
    }
    public void addSubClass(String subClass){
        this.subClasses.add(subClass);
        this.subclassNum = subClasses.size();
    }
}
