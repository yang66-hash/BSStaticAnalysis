package com.yang.parse;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
@Getter
@Setter
public class APIParseAttr {
    private Set<String> apiVersions;
    private Integer maxParamNumInSvc;
    public APIParseAttr(){
        this.apiVersions = new HashSet<>();
        this.maxParamNumInSvc = 0;
    }
}
