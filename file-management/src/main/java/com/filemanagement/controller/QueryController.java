package com.filemanagement.controller;

import com.filemanagement.requestDto.QueryRequest;
import com.filemanagement.response.ApiResponse;
import com.filemanagement.responseDto.ColumnMetadataResponseDto;
import com.filemanagement.responseDto.QueryResponseDto;
import com.filemanagement.service.QueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/query")
public class QueryController {

    private final QueryService queryService;

    public QueryController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<QueryResponseDto>> query(
            @RequestParam Long fileId,
            @RequestParam String sheetName,
            @Valid @RequestBody QueryRequest request) throws SQLException {

        QueryResponseDto result = queryService.query(fileId, sheetName, request);

        ApiResponse<QueryResponseDto> response = new ApiResponse<>();

        response.setSuccess(true);
        response.setMessage("Query executed successfully");
        response.setData(result);
        response.setTimestamp(LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/columns")
    public ResponseEntity<ApiResponse<List<ColumnMetadataResponseDto>>> getColumnMetadata(
            @RequestParam Long fileId,
            @RequestParam String sheetName) throws SQLException {

        List<ColumnMetadataResponseDto> columns =
                queryService.getColumnMetadata(fileId, sheetName);

        ApiResponse<List<ColumnMetadataResponseDto>> response =
                new ApiResponse<>();

        response.setSuccess(true);
        response.setMessage("Column metadata retrieved successfully");
        response.setData(columns);
        response.setTimestamp(LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refreshSheet(@RequestParam Long fileId,
            @RequestParam String sheetName)
            throws SQLException {

        queryService.refreshSheet(fileId, sheetName);

        ApiResponse<Void> response = new ApiResponse<>();

        response.setSuccess(true);
        response.setMessage("Sheet refreshed successfully");
        response.setData(null);
        response.setTimestamp(LocalDateTime.now());

        return ResponseEntity.ok(response);
    }
}