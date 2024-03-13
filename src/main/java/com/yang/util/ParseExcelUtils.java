package com.yang.util;

import com.yang.parse.ParseAttributes;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ParseExcelUtils {

    public static void main(String[] args) {

        List<ParseAttributes> parseAttributes = ParseExcelUtils.parseExcel("E:/work_space/newest-data.xls");
        parseAttributes.forEach(attr->{
            System.out.println(attr.toString());
        });
        outPutFinalExcel(parseAttributes);
    }

    private static List<ParseAttributes> parseExcel(String path){
        List<ParseAttributes> parseAttributes = new ArrayList<>();
        try {
            //创建工作簿
            HSSFWorkbook hssfWorkbook = new HSSFWorkbook(new FileInputStream(path));
            //获取工作簿下sheet的个数
            int sheetNum = hssfWorkbook.getNumberOfSheets();
            //遍历工作簿中的所有数据
            for(int i = 0;i<sheetNum;i++) {
                //读取第i个工作表
                HSSFSheet sheet = hssfWorkbook.getSheetAt(i);
                //获取最后一行的num，即总行数。此处从0开始
                int maxRow = sheet.getLastRowNum();
                int row = 1;
                while (row<=maxRow){
                    List<ParseAttributes> partData = new ArrayList<>();
                    ParseAttributes tmpData = convertRowToData(row++,sheet);
                    partData.add(tmpData);

                    while (row<=maxRow&&tmpData.getSysName().equals(sheet.getRow(row).getCell(0).toString())
                            &&tmpData.getReleases().equals(sheet.getRow(row).getCell(2).toString())){
                        partData.add(convertRowToData(row,sheet));
                        row++;
                    }
                    //处理微服务系统数据
                    partData.forEach(service->{
                        service.setMicroserviceNum(partData.size());
                        Map<String,Integer> serviceCall = service.getServiceCall();
                        service.setMaxServiceCall(0);
                        serviceCall.forEach((key,value)->{
                            if (value>service.getMaxServiceCall()){
                                service.setMaxServiceCall(value);
                            }
                        });
                        service.setServiceCallCate(service.getServiceCall().size());
                        service.setServiceCallPer((service.getServiceCallCate()+0.0)/service.getMicroserviceNum());
                        Map<String,Integer> serviceCalled = service.getServiceCalled();
                        service.setMaxServiceCalled(0);
                        serviceCalled.forEach((key,value)->{
                            if (value>service.getMaxServiceCalled()){
                                service.setMaxServiceCalled(value);
                            }
                        });
                        service.setServiceCalledCate(serviceCalled.size());
                        service.setServiceCalledPer((service.getServiceCalledCate()+0.0)/service.getMicroserviceNum());

                        Map<String,Integer> serviceImplCall = service.getServiceImplCall();
                        AtomicInteger implCalledNum = new AtomicInteger();
                        serviceImplCall.forEach((key,value)->{
                            implCalledNum.addAndGet(value);
                        });
                        service.setServiceImplCallNum(implCalledNum.get());
                    });

                    parseAttributes.addAll(partData);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return parseAttributes;
    }
    
    private static ParseAttributes convertRowToData(int row,HSSFSheet sheet){
        ParseAttributes data = new ParseAttributes();
        int col = 0;
        data.setSysName(sheet.getRow(row).getCell(col++).toString());
        data.setServiceName(sheet.getRow(row).getCell(col++).toString());
        data.setReleases(sheet.getRow(row).getCell(col++).toString());
        data.setCodeSize((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setEntityNum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setEntityAttributeNum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setAveEntityAttribute(sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setControllerNum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setInterfaceNum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setAbstractClassNum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setServiceClassNum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setDtoObjectNum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setAPINum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setAPIVersionNum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setMaxParaNum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setAPIVersionSet(sheet.getRow(row).getCell(col++).getStringCellValue());
        data.setDBNum((int) sheet.getRow(row).getCell(col++).getNumericCellValue());
        data.setServiceImplCall(mapStringToMap(sheet.getRow(row).getCell(col++).getStringCellValue()));
        data.setServiceCall(mapStringToMap(sheet.getRow(row).getCell(col++).getStringCellValue()));
        data.setServiceCalled(mapStringToMap(sheet.getRow(row).getCell(col++).getStringCellValue()));
        return data;
    }

    private static   Map<String,Integer> mapStringToMap(String stringMap){
        if ("{}".equals(stringMap))
            return new HashMap<>();
        Map<String,Integer> map = new HashMap<>();
        stringMap = stringMap.replace("{","").replace("}","");
        System.out.println(stringMap);
        String[] strings = stringMap.split(",");
        for (String str : strings) {
            String[] s = str.split("=");
            map.put(s[0],Integer.parseInt(s[1]));
        }
        return map;
    }


    /**
     * @param attributes 处理完的数据
     * @return 将最终处理的数据存入新的excel中
     */
    private static boolean outPutFinalExcel(List<ParseAttributes> attributes){


        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("after-process-data");
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
        cell.setCellValue("serviceImplCallNum");
        cell = row.createCell(19);
        cell.setCellValue("serviceCall");
        cell = row.createCell(20);
        cell.setCellValue("serviceCalled");
        cell = row.createCell(21);
        cell.setCellValue("maxServiceCall");
        cell = row.createCell(22);
        cell.setCellValue("serviceCallCate");
        cell = row.createCell(23);
        cell.setCellValue("serviceCallPer");
        cell = row.createCell(24);
        cell.setCellValue("maxServiceCalled");
        cell = row.createCell(25);
        cell.setCellValue("serviceCalledCate");
        cell = row.createCell(26);
        cell.setCellValue("serviceCalledPer");
        cell = row.createCell(27);
        cell.setCellValue("microserviceNum");

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
                row1.createCell(17).setCellValue(parseAttributes.getServiceImplCall().toString());
                row1.createCell(18).setCellValue(parseAttributes.getServiceImplCallNum().toString());
                row1.createCell(19).setCellValue(parseAttributes.getServiceCall().toString());
                row1.createCell(20).setCellValue(parseAttributes.getServiceCalled().toString());
                row1.createCell(21).setCellValue(parseAttributes.getMaxServiceCall().toString());
                row1.createCell(22).setCellValue(parseAttributes.getServiceCallCate().toString());
                row1.createCell(23).setCellValue(parseAttributes.getServiceCallPer().toString());
                row1.createCell(24).setCellValue(parseAttributes.getMaxServiceCalled().toString());
                row1.createCell(25).setCellValue(parseAttributes.getServiceCalledCate().toString());
                row1.createCell(26).setCellValue(parseAttributes.getServiceCalledPer().toString());
                row1.createCell(27).setCellValue(parseAttributes.getMicroserviceNum().toString());
            }catch (NullPointerException e){
                e.printStackTrace();
                System.out.println("a piece of data reading error ...");
                continue;
            }
        }
        try {
            FileOutputStream fos = new FileOutputStream("E:/work_space/final-data.xls");
            workbook.write(fos);
            workbook.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("error occurring when opening excel file ...");
            return false;
        }
        return true;

    }

}
