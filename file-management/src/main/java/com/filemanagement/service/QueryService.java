package com.filemanagement.service;

import com.filemanagement.requestDto.QueryRequest;
import com.filemanagement.responseDto.ColumnMetadataResponseDto;
import com.filemanagement.responseDto.QueryResponseDto;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface QueryService {

    void cacheSheet(Long fileId, String sheetName) throws SQLException;
    QueryResponseDto query(Long fileId, String sheetName, QueryRequest request)
            throws SQLException ;
    List<ColumnMetadataResponseDto> getColumnMetadata(Long fileId, String sheetName) throws SQLException;
    void refreshSheet(Long fileId, String sheetName) throws SQLException;
}
