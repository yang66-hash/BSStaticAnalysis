package com.yang.controller;

import com.yang.item.ExtractRequest;
import com.yang.parse.ParseAttributes;
import com.yang.util.ExtraUtils;
import com.yang.util.ParseExcelUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @description:
 * @author: xyc
 * @date: 2025-04-07 10:33
 */
@RestController
@RequestMapping("data")
public class DataController {

    @PostMapping("/features")
    public ResponseEntity<String> extractExtraData(@RequestBody ExtractRequest request) {
        String reposPath = request.getReposPath();
        String outputPath = request.getOutputPath();
        //extract raw data
        String dataPath = ExtraUtils.extraDataInDir(reposPath);

        List<ParseAttributes> parseAttributes = ParseExcelUtils.parseExcel(dataPath);
        parseAttributes.forEach(attr->{
            System.out.println(attr.toString());
        });
        ParseExcelUtils.outPutFinalCSV(parseAttributes, outputPath);

        return ResponseEntity.ok(outputPath);
    }

}