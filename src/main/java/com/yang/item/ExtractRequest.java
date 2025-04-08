package com.yang.item;

import lombok.Data;

/**
 * @description:
 * @author: xyc
 * @date: 2025-04-07 10:46
 */
@Data
public class ExtractRequest {
    private String reposPath;
    private String outputPath;
}