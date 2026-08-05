package com.filemanagement.repository;

import com.filemanagement.requestDto.QueryRequest;
import com.filemanagement.responseDto.ColumnMetadataResponseDto;
import com.filemanagement.responseDto.QueryResult;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface DuckDbRepository {

    void cacheSheet(String tableName, Path filePath, String sheetName) throws SQLException;
    QueryResult query(String tableName, QueryRequest request, List<ColumnMetadataResponseDto> metadata)
            throws SQLException;
    List<ColumnMetadataResponseDto> getColumnMetadata(String tableName) throws SQLException;
    void dropTable(String tableName) throws SQLException;
    boolean tableExists(String tableName) throws SQLException;
    void clearWorkspace() throws SQLException;
}
