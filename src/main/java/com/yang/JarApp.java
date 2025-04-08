package com.yang;

import com.yang.util.ExtraUtils;

import java.util.Arrays;

/**
 * Hello world!
 *
 */
public class JarApp
{
    public static void main( String[] args )
    {

        String reposPath = args[0];
        System.out.println("parsing input argument...");
        System.out.println("path: "+args[0]);
        if (reposPath==null){
            System.out.println("argument-reposPath error");
            System.exit(1);
        }
        String status = ExtraUtils.extraDataInDir(reposPath);
        if (!status.equals("parse error")){
            System.out.println("data extra successfully...");
        }
    }
}