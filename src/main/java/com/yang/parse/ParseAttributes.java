package com.yang.parse;

import lombok.Data;

import java.util.Map;

@Data
public class ParseAttributes {
    private String sysName;
    private String serviceName;
    private String releases;
    private Integer codeSize;
    private Integer entityNum;
    private Integer entityAttributeNum;
    private Double aveEntityAttribute;
    private Integer controllerNum;
    private Integer interfaceNum;
    private Integer abstractClassNum;
    private Integer serviceClassNum;
    private Integer dtoObjectNum;
    private Integer APINum;
    private Integer maxParaNum;
    private Integer APIVersionNum;
    private String APIVersionSet;
    private Integer DBNum;
    private Map<String,Integer> serviceImplCall;
    private Integer serviceImplCallNum;
    private Map<String,Integer> serviceCall;
    private Integer maxServiceCall;
    private Integer serviceCallCate;
    private Double serviceCallPer;
    private Map<String,Integer> serviceCalled;
    private Integer maxServiceCalled;
    private Integer serviceCalledCate;
    private Double serviceCalledPer;
    private Integer microserviceNum;
    public ParseAttributes(){}

}
