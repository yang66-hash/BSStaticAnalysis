package com.yang.util;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.yang.component.Configuration;
import com.yang.component.Pom;
import com.yang.parse.APIParseAttr;
import com.yang.parse.ParseAttributes;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.dom4j.DocumentException;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListTagCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.lib.Ref;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtraUtils {

    private final static String REGEX = "\\S*(service|Service|SERVICE)";

    /**
     * @param reposPath 抽取当前文件路径下的微服务系统信息
     * @return
     */
    public static String  extraDataInDir(String reposPath) {
        String[] strings = reposPath.split("/");
        String sysName = strings[strings.length-1];
        List<String> microservicePaths;
        Map<String, String> filePathToMicroserviceName;
        Map<String,String> feignInf;
        try {
            //获取文件夹下的被识别出的微服务
            microservicePaths = FileUtils.getServices(reposPath);
            //获取微服务系统当前微服务目录下每个微服务模块目录与名称 (名称需要解析配置文件) 的映射
            filePathToMicroserviceName = FileUtils.getFilePathToMicroserviceName(reposPath);
            feignInf = FileUtils.getFeignInf(reposPath);

        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error occurring when opening services dir ...");
            return "parse error";
        }

        // 针对每一个微服务
        List<ParseAttributes> attributes = new ArrayList<>();
        //获取微服务调用关系
        Map<String, Map<String, Integer>> microserviceCallResults = ServiceCallParserUtils.getMicroserviceCallResults(filePathToMicroserviceName,feignInf); // 微服务间调用
        //打开本地已存在的仓库 分析每个微服务
        for (String microservicePath : microservicePaths){
            ParseAttributes parseAttributes = new ParseAttributes();
            parseAttributes.setSysName(sysName);
            parseAttributes.setReleases("newest");
            parseAttributesFromSvc(parseAttributes,microservicePath,filePathToMicroserviceName,microserviceCallResults);
            attributes.add(parseAttributes);
        }

        return transData2Excel(attributes);
    }

    /**
     * @param reposPath 抽取当前文件路径下的微服务系统信息
     * @return
     */
    public static boolean  extraDataInGitRepos(String reposPath) {

        String[] strings = reposPath.split("/");
        String sysName = strings[strings.length-1];
        List<String> microservicePaths;
        Map<String, String> filePathToMicroserviceName;
        Map<String,String> feignInf;
        try {
            microservicePaths = FileUtils.getServices(reposPath);
            filePathToMicroserviceName = FileUtils.getFilePathToMicroserviceName(reposPath);
            feignInf = FileUtils.getFeignInf(reposPath);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error occurring when opening services dir ...");
            return false;
        }

        // 针对每一个微服务
        List<ParseAttributes> attributes = new ArrayList<>();

        //打开本地已存在的仓库 分析每个微服务
        try {
            File localPath = new File(reposPath);
            //打开本地已存在的仓库
            Git git = Git.open(localPath);
            ListTagCommand listTagCommand =  git.tagList();
            List<Ref> tags = listTagCommand.call();
            git.close();
            //没有版本快照
            if (tags.size()==0){
                Map<String, Map<String, Integer>> microserviceCallResults = ServiceCallParserUtils.getMicroserviceCallResults(filePathToMicroserviceName,feignInf); // 微服务间调用
                for (String microservicePath : microservicePaths){
                    ParseAttributes parseAttributes = new ParseAttributes();
                    parseAttributes.setSysName(sysName);
                    parseAttributes.setReleases("newest");
                    parseAttributesFromSvc(parseAttributes,microservicePath,filePathToMicroserviceName,microserviceCallResults);
                    attributes.add(parseAttributes);
                }
            }
            else {
                for (Ref tag : tags) {
                    //切换版本
                    try {
                        git.checkout()
                                .setName(tag.getName())
                                .call();
                        Map<String, Map<String, Integer>> microserviceCallResults = ServiceCallParserUtils.getMicroserviceCallResults(filePathToMicroserviceName,feignInf); // 微服务间调用
                        for (String microservicePath : microservicePaths){
                            ParseAttributes parseAttributes = new ParseAttributes();
                            parseAttributes.setSysName(sysName);
                            parseAttributes.setReleases(tag.getName());
                            parseAttributesFromSvc(parseAttributes,microservicePath,filePathToMicroserviceName,microserviceCallResults);
                            attributes.add(parseAttributes);
                        }
                    }catch (GitAPIException e){
                        e.printStackTrace();
                        System.out.println("error occurring when checkout new tag...");
                        continue;
                    }catch (JGitInternalException e){
                        System.out.println("JGitInternalException occurring when checkout new tag...");
                        continue;
                    }


                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        transData2Excel(attributes);
        return true;
    }


    /**
     * @param parseAttributes 存储解析信息
     * @param microservicePath 对应微服务路径
     * @param filePathToMicroserviceName 对应的配置文件路径
     * @param microserviceCallResults 微服务之间调用关系
     * 解析当前微服务的相关信息
     */
    private static void parseAttributesFromSvc(ParseAttributes parseAttributes,String microservicePath,
                                               Map<String,
                                               String> filePathToMicroserviceName,
                                               Map<String, Map<String, Integer>> microserviceCallResults){

        System.out.println("current service path: "+microservicePath);
        Set<String> dataBases = new LinkedHashSet<>();
        try {
            //获取main下的所有.java文件
            List<String> javaFiles = FileUtils.getJavaFiles(microservicePath);
            //获取当前微服务的配置文件
            List<String> applicationYamlOrProperties = FileUtils.getApplicationYamlOrProperties(microservicePath);
            //获取pom.xml文件
            List<String> pomXml = FileUtils.getPomXml(microservicePath);
            //存储配置文件配置项相关信息
            List<Configuration> configurations = new ArrayList<>();
            for (String applicationYamlOrProperty : applicationYamlOrProperties) {
                Configuration configuration = new Configuration();
                if (applicationYamlOrProperty.endsWith("yaml") || applicationYamlOrProperty.endsWith("yml")) {
                    Yaml yaml = new Yaml();
                    final Iterable<Object> objects = yaml.loadAll(new FileInputStream(applicationYamlOrProperty));
                    for (Object o : objects) {
                        FileUtils.resolveYaml(new Stack<>(), configuration.getItems(), (Map<String, Object>) o);
                    }
                } else {
                    FileUtils.resolveProperties(applicationYamlOrProperty, configuration.getItems());
                }
                configurations.add(configuration);
            }
            // 解析依赖文件
            List<Pom> poms = new ArrayList<>();
            for (String p : pomXml) {
                Pom pom = new Pom();
                MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
                Model model = mavenXpp3Reader.read(new FileInputStream(p));
                pom.setMavenModel(model);
                poms.add(pom);
            }
            // 非 Spring 官方依赖
            Set<String> dependencies = new LinkedHashSet<>();
            // 所有依赖
            Set<String> allDependencies = new LinkedHashSet<>();
            for (Pom pom : poms) {
                for (Dependency dependency : pom.getMavenModel().getDependencies()) {
                    if (!dependency.getGroupId().startsWith("org.springframework.boot")) {
                        dependencies.add(dependency.getGroupId() + "." + dependency.getArtifactId());
                    }
                    allDependencies.add(dependency.getGroupId() + "." + dependency.getArtifactId());
                }
            }
            int codeSize = 0; // 总代码行
            int entitiesFieldCount = 0; // 实体类所有属性个数
            System.out.println(allDependencies.toString());
            List<String> entityClasses = FileUtils.getJavaFilesUnderEntity(microservicePath,allDependencies); // 实体类集合
            List<String> controllerClasses = new ArrayList<>(); // 控制器类集合
            List<String> interfaces = new ArrayList<>(); // 接口
            List<String> serviceImplementationClasses = new ArrayList<>();
            List<String> abstractClasses = new ArrayList<>(); // 抽象类
            List<String> dtoClasses = JavaParserUtils.getDtoClasses(javaFiles); // 数据传输类 DTO
            Set<String> apis = new LinkedHashSet<>(); // api
            APIParseAttr apiParseAttr = JavaParserUtils.getApiInfo(javaFiles, apis); // api 版本数 以及最大参数值
            dataBases.clear();
            getDataBases(dataBases, configurations);
            // <微服务名称:<Service对象:<方法名:次数>>>
            Map<String, Map<String, Map<String, Integer>>> serviceMethodCallResults = new HashMap<>();
            String microserviceName = filePathToMicroserviceName.get(microservicePath);
            serviceMethodCallResults.put(microserviceName, new HashMap<>());
            for (String javaFile : entityClasses) {
                // 实体类和 DTO
                if (JavaParserUtils.isEntityClass(microservicePath, new File(javaFile), allDependencies)) {
                    entitiesFieldCount += JavaParserUtils.getEntityClassFieldCount(new File(javaFile));
                } else {
                    dtoClasses.add(javaFile);
                }
            }
            for (String javaFile : javaFiles) {
                File file = new File(javaFile);
                CompilationUnit compilationUnit = StaticJavaParser.parse(file);
                // 代码行
                codeSize += FileUtils.getJavaFileLinesOfCode(file);
                // 控制器类
                if (JavaParserUtils.isControllerClass(file)) {
                    controllerClasses.add(javaFile);
                    Map<String, Map<String, Integer>> serviceMethodCallOfController = JavaParserUtils.getServiceMethodCallOfController(compilationUnit);
                    // 统计 service 层方法调用次数
                    // service 对象名称
                    for (String serviceObject : serviceMethodCallOfController.keySet()) {
                        // 当前微服务模块未统计该 service 对象的方法调用
                        if (!serviceMethodCallResults.get(microserviceName).containsKey(serviceObject)) {
                            serviceMethodCallResults.get(microserviceName).put(serviceObject, new HashMap<>());
                        }
                        for (String serviceMethod : serviceMethodCallOfController.get(serviceObject).keySet()) {
                            Integer count = serviceMethodCallOfController.get(serviceObject).get(serviceMethod);
                            count += serviceMethodCallResults.get(microserviceName).get(serviceObject).getOrDefault(serviceMethod, 0);
                            serviceMethodCallResults.get(microserviceName).get(serviceObject).put(serviceMethod, count);
                        }
                    }
                }
                // 抽象类或接口
                String abstractClassOrInterface = JavaParserUtils.isAbstractClassOrInterface(file);
                if ("interface".equals(abstractClassOrInterface)) {
                    interfaces.add(javaFile);
                } else if ("abstract".equals(abstractClassOrInterface)) {
                    abstractClasses.add(javaFile);
                }
                // 服务实现类
                if ("ServiceImpl".equals(JavaParserUtils.isServiceImplementationClass(file))) {
                    serviceImplementationClasses.add(javaFile);
                }
            }
            parseAttributes.setServiceName(microserviceName);
            parseAttributes.setCodeSize(codeSize);
            parseAttributes.setEntityNum(entityClasses.size());
            parseAttributes.setEntityAttributeNum(entitiesFieldCount);
            parseAttributes.setAveEntityAttribute(entityClasses.isEmpty() ? 0 : entitiesFieldCount * 1.0 / entityClasses.size());
            parseAttributes.setControllerNum(controllerClasses.size());
            parseAttributes.setInterfaceNum(interfaces.size());
            parseAttributes.setAbstractClassNum(abstractClasses.size());
            parseAttributes.setServiceClassNum(serviceImplementationClasses.size());
            parseAttributes.setDtoObjectNum(dtoClasses.size());
            parseAttributes.setAPINum(apis.size());
            parseAttributes.setAPIVersionNum(apiParseAttr.getApiVersions().size());
            parseAttributes.setMaxParaNum(apiParseAttr.getMaxParamNumInSvc());
            parseAttributes.setAPIVersionSet(apiParseAttr.getApiVersions().toString());
            parseAttributes.setDBNum(dataBases.size());
            //统计各个service的接口的调用次数
            Map<String,Integer> serviceImplCall = new HashMap<>();
            Map<String,Map<String,Integer>> funcCallMap = serviceMethodCallResults.get(microserviceName);
            for (Map.Entry<String,Map<String,Integer>> outerMap:
                    funcCallMap.entrySet()) {
                Map<String,Integer> outEntry = outerMap.getValue();
                for (Map.Entry<String,Integer> insideMap:
                        outEntry.entrySet()) {
                    serviceImplCall.put(outerMap.getKey()+"-"+insideMap.getKey(),insideMap.getValue());
                }
            }
            parseAttributes.setServiceImplCall(serviceImplCall);
            parseAttributes.setServiceCall(microserviceCallResults.get(parseAttributes.getServiceName()));
            //用于解析当前服务被哪些服务调用过
            Map<String,Integer> serviceCalled = new HashMap<>();
            for (Map.Entry<String,Map<String,Integer>> mapOut:
                    microserviceCallResults.entrySet()) {
                Map<String,Integer> map = mapOut.getValue();
                for (String key:
                        map.keySet()) {
                    String serviceKey = key;
                    Matcher matcher = Pattern.compile(REGEX).matcher(key + "");
                    if (matcher.find())
                        serviceKey = matcher.group();
                    if (parseAttributes.getServiceName()!=null&&parseAttributes.getServiceName().equals(serviceKey)){
                        serviceCalled.put(mapOut.getKey(),map.get(key));
                    }
                }
            }
            parseAttributes.setServiceCalled(serviceCalled);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }


    private static String transData2Excel(List<ParseAttributes> attributes){
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("data");
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell = row.createCell(0);
        cell.setCellValue("sysName");
        cell = row.createCell(1);
        cell.setCellValue("serviceName");
        cell = row.createCell(2);
        cell.setCellValue("releases");
        cell = row.createCell(3);
        cell.setCellValue("codeSize");
        cell = row.createCell(4);
        cell.setCellValue("entityNum");
        cell = row.createCell(5);
        cell.setCellValue("entityAttributeNum");
        cell = row.createCell(6);
        cell.setCellValue("aveEntityAttribute");
        cell = row.createCell(7);
        cell.setCellValue("controllerNum");
        cell = row.createCell(8);
        cell.setCellValue("interfaceNum");
        cell = row.createCell(9);
        cell.setCellValue("abstractClassNum");
        cell = row.createCell(10);
        cell.setCellValue("serviceClassNum");
        cell = row.createCell(11);
        cell.setCellValue("dtoObjectNum");
        cell = row.createCell(12);
        cell.setCellValue("APINum");
        cell = row.createCell(13);
        cell.setCellValue("APIVersionNum");
        cell = row.createCell(14);
        cell.setCellValue("maxParaNum");
        cell = row.createCell(15);
        cell.setCellValue("APIVersionSet");
        cell = row.createCell(16);
        cell.setCellValue("DBNum");
        cell = row.createCell(17);
        cell.setCellValue("serviceImplCall");
        cell = row.createCell(18);
        cell.setCellValue("serviceCall");
        cell = row.createCell(19);
        cell.setCellValue("serviceCalled");

        for (int i = 0; i < attributes.size(); i++) {
            try {
                HSSFRow row1 = sheet.createRow(i+1);
                System.out.println("=================================================");
                System.out.println(attributes.get(i).toString());
                System.out.println("=================================================");
                ParseAttributes parseAttributes = attributes.get(i);
                row1.createCell(0).setCellValue(parseAttributes.getSysName());
                row1.createCell(1).setCellValue(parseAttributes.getServiceName());
                row1.createCell(2).setCellValue(parseAttributes.getReleases());
                row1.createCell(3).setCellValue(parseAttributes.getCodeSize());
                row1.createCell(4).setCellValue(parseAttributes.getEntityNum());
                row1.createCell(5).setCellValue(parseAttributes.getEntityAttributeNum());
                row1.createCell(6).setCellValue(parseAttributes.getAveEntityAttribute());
                row1.createCell(7).setCellValue(parseAttributes.getControllerNum());
                row1.createCell(8).setCellValue(parseAttributes.getInterfaceNum());
                row1.createCell(9).setCellValue(parseAttributes.getAbstractClassNum());
                row1.createCell(10).setCellValue(parseAttributes.getServiceClassNum());
                row1.createCell(11).setCellValue(parseAttributes.getDtoObjectNum());
                row1.createCell(12).setCellValue(parseAttributes.getAPINum());
                row1.createCell(13).setCellValue(parseAttributes.getAPIVersionNum());
                row1.createCell(14).setCellValue(parseAttributes.getMaxParaNum());
                row1.createCell(15).setCellValue(parseAttributes.getAPIVersionSet());
                row1.createCell(16).setCellValue(parseAttributes.getDBNum());
                row1.createCell(17).setCellValue( parseAttributes.getServiceImplCall().toString());
                row1.createCell(18).setCellValue(parseAttributes.getServiceCall().toString());
                row1.createCell(19).setCellValue(parseAttributes.getServiceCalled().toString());
            }catch (NullPointerException e){
                e.printStackTrace();
                System.out.println("a piece of data reading error ...");
                continue;
            }
        }
        String dataPath = "D:/work_space/"+attributes.get(0).getSysName()+"-data.xls";
        try {
            System.out.println("attributes.get(0).getSysName()" +attributes.get(0).getSysName());
            FileOutputStream fos = new FileOutputStream(dataPath);
            System.out.println("./"+System.currentTimeMillis()+"-"+attributes.get(0).getSysName()+"-data.xls");
            workbook.write(fos);
            workbook.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error occurring when opening excel file ...");
            return "parse error";
        }
        return dataPath;

    }



    private static void getDataBases(Set<String> dataBases, List<Configuration> configurations) {
        String pattern = "mysql://";
        for (Configuration configuration : configurations) {
            for (String key : configuration.getItems().keySet()) {
                String value = configuration.get(key);
                String dataBase = "";
                if (value.contains(pattern)) {
                    int startIndex = value.indexOf(pattern) + 8;
                    int endIndex = value.contains("?") ? value.indexOf("?") : value.length();
                    if (value.contains("///")) {
                        startIndex = value.indexOf("///") + 3;
                        dataBase = "localhost:3306/" + value.substring(startIndex, endIndex);
                    }
                    else if (value.contains("127.0.0.1")){
                        startIndex = value.indexOf("//") + 2;
                        dataBase = value.substring(startIndex, endIndex);
                        dataBase = dataBase.replace("localhost", "127.0.0.1");
                    }
                    else {
                        dataBase = value.substring(startIndex, endIndex);
                    }
                }
                if (!"".equals(dataBase)) {
                    dataBases.add(dataBase);
                }
            }
        }
    }


}

