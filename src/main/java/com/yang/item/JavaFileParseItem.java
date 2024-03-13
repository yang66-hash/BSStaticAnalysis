package com.yang.item;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Cocoicobird
 * @version 1.0
 * @description 存储 JavaParser 解析出的信息
 */
@Data
public class JavaFileParseItem {
    private List<FieldDeclaration> fieldDeclarations;
    private List<MethodDeclaration> methodDeclarations;

    public JavaFileParseItem(){
        this.fieldDeclarations = new ArrayList<>();
        this.methodDeclarations = new ArrayList<>();
    }
}
