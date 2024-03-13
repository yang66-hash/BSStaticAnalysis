package com.yang.util;


import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.ParserCollectionStrategy;
import com.github.javaparser.utils.ProjectRoot;
import com.yang.component.Configuration;
import com.yang.item.ServiceCutItem;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Cocoicobird
 * @version 1.0
 */
public class FileUtils {

    private static final String regex = "[/\\\\](?i)(entity|pojo|model|domain|bean)[/\\\\]";

    /**
     * 获取某一目录下的所有配置文件路径
     * @param directory 微服务路径
     * @return 返回 directory 下的所有配置文件路径列表
     */
    public static List<String> getApplicationYamlOrProperties(String directory) throws IOException {
        Path parent = Paths.get(directory);
        int maxDepth = 10;
        Stream<Path> stream = Files.find(parent, maxDepth, (filePath, attributes) -> true);
        List<String> applicationYamlOrProperties = stream.sorted().map(String::valueOf).filter(filePath -> {
            return (String.valueOf(filePath).toLowerCase().endsWith("application.yml")
                    || String.valueOf(filePath).toLowerCase().endsWith("application.yaml")
                    || String.valueOf(filePath).toLowerCase().endsWith("bootstrap.yml")
                    || String.valueOf(filePath).toLowerCase().endsWith("bootstrap.yaml")
                    || String.valueOf(filePath).toLowerCase().endsWith("application.properties"))
                    && !String.valueOf(filePath).toLowerCase().contains("target");
        }).collect(Collectors.toList());
        return applicationYamlOrProperties;
    }

    /**
     * 解析 yaml 里的数据数据存入到 map 中
     * @param stack 存储 key 的栈
     * @param map 用于存储配置文件 key-value 的映射
     * @param yaml 读取的待解析的 yaml 文件内容
     */
    public static void resolveYaml(Stack<String> stack, Map<String, String> map, Map<String, Object> yaml) {
        for (String key : yaml.keySet()) {
            Object value = yaml.get(key);
            stack.add(key);
            if (value instanceof Map) {
                resolveYaml(stack, map, (Map<String, Object>) value);
            } else {
                map.put(String.join(".", stack),value==null?"null":value.toString());
                stack.pop();
            }
        }
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    /**
     * 解析以properties结尾的配置文件 filePath 存入 map
     * @param filePath 以properties结尾的配置文件路径
     * @param map 用于存储配置文件 key-value 的映射
     */
    public static void resolveProperties(String filePath, Map<String, String> map) throws IOException {
        Properties properties = new Properties();
        properties.load(new BufferedInputStream(new FileInputStream(filePath)));
        for (Object key : properties.keySet()) {
            map.put((String) key, properties.getProperty((String) key));
        }
    }
    /**
     * 获取当前路径中所有的 pom.xml 文件
     * @param directory 路径
     * @return pom.xml 文件的路径列表
     */
    public static List<String> getPomXml(String directory) throws IOException {
        Path parent = Paths.get(directory);
        int maxDepth = 10;
        Stream<Path> stream = Files.find(parent, maxDepth,
                (filepath, attributes) -> String.valueOf(filepath).contains("pom.xml"));
        List<String> pomXml = stream.sorted().map(String::valueOf).filter(filepath ->{
            return !String.valueOf(filepath).toLowerCase().contains(".mvn")
                    && !String.valueOf(filepath).toLowerCase().contains("gradle");
        }).collect(Collectors.toList());
        return  pomXml;
    }

    /**
     * 获取本机 directory 项目路径下的所有微服务模块的完整路径
     * @param directory 微服务系统路径
     * @return 微服务系统所有微服务模块的完整路径
     */
    public static List<String> getServices(String directory) throws IOException {
        File[] files = new File(directory).listFiles();
        List<String> services = new ArrayList<>();
        if (files == null) {
            return services;
        }
        for (File file : files) {
            //该文件夹下包含yaml或properties文件
            if (file.isDirectory() && FileUtils.getApplicationYamlOrProperties(file.getAbsolutePath()).size() != 0) {
                services.add(file.getAbsolutePath());
            }
        }
        return services;
    }

    /**
     * 获取 directory 目录下所有的 .java 文件，排除 test 目录中的文件
     * @param directory 待检索目录
     * @return .java 文件路径的列表
     */
    public static List<String> getJavaFiles(String directory) throws IOException {
        Path parent = Paths.get(directory);
        List<String> javaFiles;
        int maxDepth = 15;
        Stream<Path> stream = Files.find(parent, maxDepth,
                (filePath, attributes) -> String.valueOf(filePath).endsWith(".java"));
        // 忽略 test 目录下的 .java 文件，但是该目录以外的类名可以包含 test 或者 Test
        javaFiles = stream.sorted().map(String::valueOf).filter(filepath ->
                        (!String.valueOf(filepath).contains("\\test\\")
                                && !String.valueOf(filepath).contains("/test/"))).collect(Collectors.toList());
        return javaFiles;
    }

    /**
     * 获取模块下 entity 目录下的所有文件 主要的实体类实现
     * @param directory 微服务模块
     * @param allDependencies
     */
    public static List<String> getJavaFilesUnderEntity(String directory, Set<String> allDependencies) throws IOException, DocumentException {

        List<String> javaFilesUnderEntity = new ArrayList<>();
        List<String> javaFiles = FileUtils.getJavaFiles(directory);
        System.out.println("=====================================================");
        for (String javaFile : javaFiles) {
            Matcher matcher1 = Pattern.compile(regex).matcher(javaFile);
            if (matcher1.find()) {
                //只是添加类
                CompilationUnit compilationUnit = StaticJavaParser.parse(new File(javaFile));
                ClassOrInterfaceDeclaration ci = compilationUnit.findFirst(ClassOrInterfaceDeclaration.class).orElse(null);
                if (ci!=null&&!ci.isInterface()&&JavaParserUtils.isEntityClass(directory,new File(javaFile),allDependencies)){
                    System.out.println(javaFile.toString()+"==============================");
                    javaFilesUnderEntity.add(javaFile);
                }

            }
        }
        return javaFilesUnderEntity;
    }

    /**
     * 获取 service 下的实体类
     * @param directory 微服务模块目录
     */
    public static Set<String> getServiceEntities(String directory, Set<String> dependencies) throws IOException, DocumentException {
        List<String> javaFiles = getJavaFiles(directory);
        Set<String> serviceEntities = new LinkedHashSet<>();
        for (String javaFile : javaFiles) {
            File file = new File(javaFile);
            if (javaFile.contains("/entity/") || javaFile.contains("\\entity\\")
                    || javaFile.contains("/domain/") || javaFile.contains("\\domain\\")
                    || javaFile.contains("/bean/") || javaFile.contains("\\bean\\")) {
                serviceEntities.add(javaFile);
            }
        }
        return serviceEntities;
    }

    /**
     * 获取系统所有微服务模块的实体类数量
     * @param filePathToMicroserviceName 微服务模块路径与微服务名称的映射
     */
    public static List<ServiceCutItem> getSystemServiceCuts(Map<String, String> filePathToMicroserviceName) throws IOException, XmlPullParserException, DocumentException {
        List<ServiceCutItem> systemServiceCuts = new ArrayList<>();
        for (String directory : filePathToMicroserviceName.keySet()) {
            ServiceCutItem serviceCut = new ServiceCutItem();
            serviceCut.setMicroserviceName(filePathToMicroserviceName.get(directory));
            serviceCut.setEntityCount(getServiceEntities(directory, getMicroserviceDependencies(directory)).size());
            systemServiceCuts.add(serviceCut);
        }
        return systemServiceCuts;
    }

    /**
     * 获取微服务模块下的静态资源文件
     * @param directory 微服务模块目录
     */
    public static List<String> getStaticFiles(String directory) throws IOException {
        Path parent = Paths.get(directory);
        List<String> staticFiles;
        int maxDepth = 15;
        Stream<Path> stream = Files.find(parent, maxDepth,
                (filePath, attributes) -> (String.valueOf(filePath).contains("html")
                        || String.valueOf(filePath).contains("js")));

        staticFiles = stream.sorted().map(String::valueOf).filter(filePath -> {
            return String.valueOf(filePath).contains("\\resources\\") || String.valueOf(filePath).contains("/resources/");
        }).collect(Collectors.toList());
        return  staticFiles;
    }

    /**
     * 获取 directory 这个微服务系统目录下每个微服务模块目录与名称 (名称需要解析配置文件) 的映射
     * @param directory 微服务系统目录
     */
    public static Map<String, String> getFilePathToMicroserviceName(String directory) throws IOException {
        Map<String, String> filePathToMicroserviceName = new HashMap<>();
        List<String> services = getServices(directory);
        for (String service : services) {
            List<String> applicationYamlOrProperties = getApplicationYamlOrProperties(service);
            String microserviceName = "";
            for (String applicationYamlOrProperty : applicationYamlOrProperties) {
                Configuration configuration = new Configuration();
                if (applicationYamlOrProperty.endsWith("yaml") || applicationYamlOrProperty.endsWith("yml")) {
                    Yaml yaml = new Yaml();
                    final Iterable<Object> objects = yaml.loadAll(new FileInputStream(applicationYamlOrProperty));
                    for (Object o : objects) {
                        FileUtils.resolveYaml(new Stack<>(), configuration.getItems(), (Map<String, Object>) o);
                    }
                } else {
                    resolveProperties(applicationYamlOrProperty, configuration.getItems());
                }
                //确定其微服务名
                String[] serviceSplit = service.split("\\\\|/");
//                microserviceName = serviceSplit[serviceSplit.length-1];
                microserviceName = configuration.getItems().getOrDefault("spring.application.name",serviceSplit[serviceSplit.length-1]);
            }
            filePathToMicroserviceName.put(service, microserviceName);
        }
        return filePathToMicroserviceName;
    }
//
//    /**
//     * 获取 directory 这个微服务系统目录下每个微服务名称 (根据路径截取) 与路径的映射
//     * @param directory 微服务系统目录
//     */
//    public static Map<String, String> getMicroserviceNameToFilePath(String directory) {
//        Map<String, String> microserviceNameToFilePath = new HashMap<>();
//        File file = new File(directory);
//        File[] files = file.listFiles();
//        if (files != null) {
//            for (File value : files) {
//                if (value.isDirectory()) {
//                    String filePath = value.toString();
//                    String microserviceName = filePath.substring(filePath.lastIndexOf(File.separator) + 1);
//                    Matcher matcher = Pattern.compile("(service|Service)").matcher(microserviceName);
//                    if (matcher.find()) {
//                        microserviceNameToFilePath.put(microserviceName, filePath);
//                    }
//                }
//            }
//        }
//        return microserviceNameToFilePath;
//    }

    /**
     * 获取项目包名，根据启动类
     * @param directory 微服务所在目录
     */
    public static String getPackageName(String directory) throws IOException {
        Path parent = Paths.get(directory);
        int maxDepth = 10;
        String packageName = "";
        List<String> javaFiles = Files.find(parent, maxDepth, (filepath, attributes) -> String.valueOf(filepath).endsWith("Application.java"))
                .map(Path::toString)
                .collect(Collectors.toList());
        for (String javaFile : javaFiles) {
            if (javaFile.contains("/src/main/java/"))
                packageName = javaFile.substring(javaFile.indexOf("java/") + 5, javaFile.lastIndexOf("/")).replace('/', '.');
            break;
        }
        return packageName;
    }

    /**
     * 计算一个 .java 文件的代码行数 去除空行、注释
     * @param javaFile .java 文件
     * @return 代码行数
     */
    public static int getJavaFileLinesOfCode(File javaFile) {
        int linesOfCode = 0;
        BufferedReader bufferedReader = null;
        boolean comment = false;
        try {
            bufferedReader = new BufferedReader(new FileReader(javaFile));
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                line = line.trim();
                if ("".equals(line))
                    continue;
                if (line.startsWith("//"))
                    continue;
                if (line.startsWith("/*") && !line.endsWith("*/")) {
                    comment = true;
                    continue;
                }
                if (comment) {
                    if (line.endsWith("*/"))
                        comment = false;
                    continue;
                }
                linesOfCode++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return linesOfCode;
    }

    /**
     * 获取微服务模块下的 Mapper 映射文件
     * @param directory 模块路径
     * @return 路径和 Mapper 的 Document 对象映射
     */
    public static Map<String, Document> getMappers(String directory) throws IOException, DocumentException {
        Map<String, Document> filePathToMapperXml = new HashMap<>();
        SAXReader saxReader = new SAXReader();
        Path parent = Paths.get(directory);
        int maxDepth = 10;
        List<String> mappers = Files.find(parent, maxDepth, (filepath, attributes) ->
                String.valueOf(filepath).toLowerCase().contains("mapper.xml"))
                .map(Path::toString)
                .collect(Collectors.toList());
        System.out.println(":::::::::::::::::::::::::::::::::::::::::;");
        for (String mapper : mappers) {
            Document mapperDocument = saxReader.read(new File(mapper));
            filePathToMapperXml.put(mapper, mapperDocument);
        }
        return filePathToMapperXml;
    }

    public static Set<String> getMicroserviceDependencies(String directory) throws IOException, XmlPullParserException {
        List<String> pomXml = FileUtils.getPomXml(directory);
        Set<String> microserviceDependencies = new LinkedHashSet<>();
        for (String p : pomXml) {
            MavenXpp3Reader mavenXpp3Reader = new MavenXpp3Reader();
            Model model = mavenXpp3Reader.read(new FileInputStream(p));
            for (Dependency dependency : model.getDependencies()) {
                microserviceDependencies.add(dependency.getGroupId() + "." + dependency.getArtifactId());
            }
        }
        return microserviceDependencies;
    }

    public static Map<String, String> getFeignInf(String reposPath) {
        Map<String,String> feignInf = new HashMap<>();
        Path microSysRoot = Paths.get(reposPath);
        // 一次性处理整个项目的 .java 文件
        ProjectRoot projectRoot = new ParserCollectionStrategy().collect(microSysRoot);
        projectRoot.getSourceRoots().forEach(sourceRoot -> {
            try {
                sourceRoot.tryToParse();
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<CompilationUnit> compilationUnits = sourceRoot.getCompilationUnits();

            //针对每个微服务的java文件
            for (CompilationUnit compilationUnit : compilationUnits) {
                new FeignInterfaceVisitor().visit(compilationUnit,feignInf);
            }
        });
        return feignInf;
    }
    private static class FeignInterfaceVisitor extends VoidVisitorAdapter<Map<String,String>> {
        @Override
        public void visit(CompilationUnit compilationUnit, Map<String,String> arg) {
            compilationUnit.findAll(ClassOrInterfaceDeclaration.class).forEach(cls->{
                cls.getAnnotations().forEach(annotationExpr -> {
                    if ("FeignClient".equals(annotationExpr.getNameAsString())){
                        NormalAnnotationExpr annotationExpr1 = (NormalAnnotationExpr) annotationExpr;
                        String value =  annotationExpr1.getPairs().stream()
                                        .filter(pair->pair.getNameAsString().equals("name"))
                                        .map(pair->pair.getValue().toString())
                                        .findFirst()
                                        .orElse(null);
                        if(value==null){
                            value =  annotationExpr1.getPairs().stream()
                                    .filter(pair->pair.getNameAsString().equals("value"))
                                    .map(pair->pair.getValue().toString())
                                    .findFirst()
                                    .orElse("PARSE ERROR");
                        }
                        value = value.replaceAll("^\"|\"$","");
                        arg.put(cls.getNameAsString(), value);

                    }
                });
            });
        }
    }


    public static void main(String[] args) {
        try {
            Set<String> list = FileUtils.getMicroserviceDependencies("E:/work_space/apollo/apollo-portal");
            list.forEach(value->{
                System.out.println(value);
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        }
    }


}
