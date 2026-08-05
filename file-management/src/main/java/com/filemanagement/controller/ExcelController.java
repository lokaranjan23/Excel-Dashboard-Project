package com.filemanagement.controller;

import com.filemanagement.response.ApiResponse;
import com.filemanagement.service.ExcelService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/excel")
public class ExcelController {

    private final ExcelService excelService;

    public ExcelController(ExcelService excelService) {
        this.excelService = excelService;
    }

    @GetMapping("/{fileId}/sheets")
    public ResponseEntity<ApiResponse<List<String>>> getSheetNames(
            @PathVariable Long fileId) {

        List<String> sheetNames = excelService.getSheetNames(fileId);

        ApiResponse<List<String>> response = new ApiResponse<>();

        response.setSuccess(true);
        response.setMessage("Sheet names retrieved successfully");
        response.setData(sheetNames);
        response.setTimestamp(LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}
