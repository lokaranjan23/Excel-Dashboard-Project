package com.filemanagement.service.impl;

import com.filemanagement.entity.UploadedFile;
import com.filemanagement.exception.*;
import com.filemanagement.repository.DuckDbRepository;
import com.filemanagement.repository.UploadedFileRepository;
import com.filemanagement.requestDto.FilterRequest;
import com.filemanagement.requestDto.QueryRequest;
import com.filemanagement.requestDto.RangeFilterRequest;
import com.filemanagement.responseDto.ColumnMetadataResponseDto;
import com.filemanagement.responseDto.QueryResponseDto;
import com.filemanagement.responseDto.QueryResult;
import com.filemanagement.service.QueryService;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class QueryServiceImpl implements QueryService {

    @Value("${app.upload-dir}")
    private String uploadDirectory;

    private final UploadedFileRepository uploadedFileRepository;
    private final DuckDbRepository duckDbRepository;
    private final ExcelServiceImpl excelService;

    public QueryServiceImpl(UploadedFileRepository uploadedFileRepository, DuckDbRepository duckDbRepository, ExcelServiceImpl excelService) {
        this.uploadedFileRepository = uploadedFileRepository;
        this.duckDbRepository = duckDbRepository;
        this.excelService = excelService;
    }

    private static final Set<String> SUPPORTED_RANGE_TYPES = Set.of(
            "INTEGER",
            "BIGINT",
            "SMALLINT",
            "TINYINT",
            "DOUBLE",
            "FLOAT",
            "REAL",
            "DECIMAL",
            "NUMERIC",
            "DATE",
            "TIMESTAMP"
    );



    private String getTableName(Long fileId, String sheetName) {

        return "file_" + fileId + "_" + sheetName.replace(" ", "_");
    }

    private void validateSheet(Long fileId, String sheetName) {

        List<String> sheets = excelService.getSheetNames(fileId);

        if (!sheets.contains(sheetName)) {
            throw new InvalidSheetException("Invalid sheet name: " + sheetName);
        }
    }

    @Override
    public void cacheSheet(Long fileId, String sheetName) throws SQLException {

        validateSheet(fileId,sheetName);
        UploadedFile uploadedFile = uploadedFileRepository.findById(fileId)
                        .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        Path filePath = Paths.get(uploadDirectory, uploadedFile.getStoredName());

        if (!Files.exists(filePath)) {
            throw new StoredFileNotFoundException("Stored file not found on disk");
        }

        String tableName = getTableName(fileId, sheetName);

        duckDbRepository.cacheSheet(tableName,filePath,sheetName);
    }

    @Override
    public QueryResponseDto query(Long fileId, String sheetName, QueryRequest request)
            throws SQLException {

        validateSheet(fileId,sheetName);

        String tableName = getTableName(fileId, sheetName);

        if (!duckDbRepository.tableExists(tableName)) {
            cacheSheet(fileId, sheetName);
        }
        List<ColumnMetadataResponseDto> metadata = getColumnMetadata(fileId, sheetName);


        validateColumns(fileId, sheetName, request);
        validateRangeFilters(fileId, sheetName,request);

        QueryResult queryResult = duckDbRepository.query(tableName, request,metadata);

        QueryResponseDto response = new QueryResponseDto();

        response.setContent(queryResult.getRows());

        response.setPage(request.getPage());

        response.setSize(request.getSize());

        response.setTotalElements(
                queryResult.getTotalElements()
        );

        response.setTotalPages((int) Math.ceil((double) queryResult.getTotalElements()
                                / request.getSize()));

        response.setFirst(request.getPage() == 0);
        response.setLast(request.getPage() >= response.getTotalPages() - 1);

        return response;
    }

    @Override
    public List<ColumnMetadataResponseDto> getColumnMetadata(Long fileId, String sheetName) throws SQLException {

        validateSheet(fileId,sheetName);

        String tableName = getTableName(fileId, sheetName);

        if (!duckDbRepository.tableExists(tableName)) {
            cacheSheet(fileId, sheetName);
        }

        return duckDbRepository.getColumnMetadata(tableName);
    }

    private void validateColumns(Long fileId, String sheetName,
            QueryRequest request) throws SQLException {

        List<ColumnMetadataResponseDto> columns = getColumnMetadata(fileId, sheetName);

        Set<String> validColumns = columns.stream()
                .map(ColumnMetadataResponseDto::getColumnName)
                .collect(Collectors.toSet());


        // Sort Column
        if (request.getSortColumn() != null &&
                !request.getSortColumn().isBlank() &&
                !validColumns.contains(request.getSortColumn())) {

            throw new InvalidColumnException(
                    "Invalid sort column: " + request.getSortColumn()
            );
        }

        // Search Filters
        if (request.getFilters() != null) {

            for (FilterRequest filter : request.getFilters()) {

                if (!validColumns.contains(filter.getColumn())) {

                    throw new InvalidColumnException(
                            "Invalid filter column: " +
                                    filter.getColumn());
                }
            }
        }

        // Range Filters
        if (request.getRangeFilters() != null) {

            for (RangeFilterRequest filter : request.getRangeFilters()) {

                if (!validColumns.contains(filter.getColumn())) {

                    throw new InvalidColumnException(
                            "Invalid range filter column: " +
                                    filter.getColumn());
                }
            }
        }
    }

    private void validateRangeFilters(
            Long fileId,
            String sheetName,
            QueryRequest request) throws SQLException {

        if (request.getRangeFilters() == null ||
                request.getRangeFilters().isEmpty()) {
            return;
        }

        List<ColumnMetadataResponseDto> metadata =
                getColumnMetadata(fileId, sheetName);

        Map<String, String> columnTypes = metadata.stream()
                .collect(Collectors.toMap(
                        ColumnMetadataResponseDto::getColumnName,
                        ColumnMetadataResponseDto::getDataType
                ));

        for (RangeFilterRequest filter : request.getRangeFilters()) {

            String dataType = columnTypes.get(filter.getColumn()).toUpperCase();

            // Validate supported data type
            if (!SUPPORTED_RANGE_TYPES.contains(dataType)) {

                throw new InvalidRangeException(
                        "Range filter is not supported for column: "
                                + filter.getColumn()
                );
            }

            Object minValue = filter.getMinValue();
            Object maxValue = filter.getMaxValue();

            // At least one bound must be present
            if (minValue == null && maxValue == null) {

                throw new InvalidRangeException(
                        "Either minimum value or maximum value must be provided for column: "
                                + filter.getColumn()
                );
            }

            // Validate individual values
            if (minValue != null) {
                validateRangeValue(
                        dataType,
                        minValue,
                        filter.getColumn()
                );
            }

            if (maxValue != null) {validateRangeValue(
                        dataType,
                        maxValue,
                        filter.getColumn()
                );
            }

            // Compare only when both are present
            if (minValue != null && maxValue != null) {

                switch (dataType) {

                    case "INTEGER":
                    case "BIGINT":
                    case "SMALLINT":
                    case "TINYINT":
                    case "DOUBLE":
                    case "FLOAT":
                    case "REAL":
                    case "DECIMAL":
                    case "NUMERIC":

                        double min = Double.parseDouble(minValue.toString());
                        double max = Double.parseDouble(maxValue.toString());

                        if (min > max) {

                            throw new InvalidRangeException(
                                    "Minimum value cannot be greater than maximum value for column: "
                                            + filter.getColumn()
                            );
                        }

                        break;

                    case "DATE":

                        LocalDate minDate =
                                LocalDate.parse(minValue.toString());

                        LocalDate maxDate =
                                LocalDate.parse(maxValue.toString());

                        if (minDate.isAfter(maxDate)) {

                            throw new InvalidRangeException(
                                    "Minimum date cannot be greater than maximum date for column: "
                                            + filter.getColumn()
                            );
                        }

                        break;

                    case "TIMESTAMP":

                        LocalDateTime minTimestamp =
                                LocalDateTime.parse(minValue.toString());

                        LocalDateTime maxTimestamp =
                                LocalDateTime.parse(maxValue.toString());

                        if (minTimestamp.isAfter(maxTimestamp)) {

                            throw new InvalidRangeException(
                                    "Minimum timestamp cannot be greater than maximum timestamp for column: "
                                            + filter.getColumn()
                            );
                        }

                        break;
                }
            }
        }
    }

    private void validateRangeValue(
            String dataType,
            Object value,
            String column) {

        try {

            switch (dataType) {

                case "INTEGER":
                case "BIGINT":
                case "SMALLINT":
                case "TINYINT":

                    Integer.parseInt(value.toString());
                    break;

                case "DOUBLE":
                case "FLOAT":
                case "REAL":
                case "DECIMAL":
                case "NUMERIC":

                    Double.parseDouble(value.toString());
                    break;

                case "DATE":

                    LocalDate.parse(value.toString());
                    break;

                case "TIMESTAMP":

                    LocalDateTime.parse(value.toString());
                    break;
            }

        } catch (Exception ex) {

            throw new InvalidRangeException(
                    "Invalid value '" + value +
                            "' for column '" +
                            column + "'"
            );
        }
    }

    @Override
    public void refreshSheet(Long fileId, String sheetName) throws SQLException {

        validateSheet(fileId,sheetName);

        String tableName = getTableName(fileId, sheetName);

        duckDbRepository.dropTable(tableName);

        cacheSheet(fileId, sheetName);
    }



}
