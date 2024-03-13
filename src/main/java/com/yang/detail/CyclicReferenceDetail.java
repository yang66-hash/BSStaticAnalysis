package com.yang.detail;

import com.yang.item.CyclicReferenceItem;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Cocoicobird
 * @version 1.0
 */
@Data
public class CyclicReferenceDetail {
    public Map<String, CyclicReferenceItem> cyclicReferences;
    public CyclicReferenceDetail(){
        this.cyclicReferences = new HashMap<>();
    }
    public void addCyclicReference(String microserviceName, String superClass, String subClass){
        if (cyclicReferences.get(subClass) == null){
            cyclicReferences.put(superClass, new CyclicReferenceItem(microserviceName, superClass));
        }
        cyclicReferences.get(superClass).addSubClass(subClass);
    }
}
