package com.yang;

import com.yang.util.ExtraUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Application {


    public static void main(String[] args) {

        String reposPath = "E:/work_space/app2/Scblogs";
//
        ExtraUtils.extraDataInDir(reposPath);

//
//        String pattern = "/(?i)v\\d+(\\.\\d+)?";
//        Pattern r = Pattern.compile(pattern);
//        Matcher m = r.matcher("/V5.2.5/");
//        Matcher n = r.matcher("/v5.2");
//        if (m.find())
//            System.out.println(true+"----");
//        if(n.find())
//            System.out.println(true);

    }
}
