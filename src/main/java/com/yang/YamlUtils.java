package com.yang;

import lombok.SneakyThrows;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class YamlUtils {


    @SneakyThrows
    public static void replaceCharWithCh(String reposPath,Character ch){
        Path parent = Paths.get(reposPath);
        int depth = 10;
        Stream<Path> stream = Files.find(parent,depth,(filePath,attributes)->true);
        List<String> application = stream.sorted().map(String::valueOf).filter(filePath->{
            return (String.valueOf(filePath).toLowerCase().endsWith("application.yml")
                    || String.valueOf(filePath).toLowerCase().endsWith("application.yaml")
                    || String.valueOf(filePath).toLowerCase().endsWith("bootstrap.yml")
                    || String.valueOf(filePath).toLowerCase().endsWith("bootstrap.yaml")
                    || String.valueOf(filePath).toLowerCase().endsWith("application.properties"))
                    && !String.valueOf(filePath).toLowerCase().contains("target");
        }).collect(Collectors.toList());
        application.forEach(value->{

            try (FileReader fileReader = new FileReader(value);
                 BufferedReader bufferedReader = new BufferedReader(fileReader);
                 FileWriter fileWriter = new FileWriter(value + ".tmp");
                 BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String replacedLine = line.replace('@', ch);
                    System.out.println(replacedLine);
                    bufferedWriter.write(replacedLine);
                    bufferedWriter.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                Files.move(Paths.get(value + ".tmp"), Paths.get(value), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args) {
        YamlUtils.replaceCharWithCh("E:/work_space/gpmall",' ');

    }
}
