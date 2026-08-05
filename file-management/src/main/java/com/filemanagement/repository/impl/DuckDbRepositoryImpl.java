package com.filemanagement.repository.impl;

import com.filemanagement.repository.DuckDbRepository;
import com.filemanagement.requestDto.FilterRequest;
import com.filemanagement.requestDto.QueryRequest;
import com.filemanagement.requestDto.RangeFilterRequest;
import com.filemanagement.responseDto.ColumnMetadataResponseDto;
import com.filemanagement.responseDto.QueryResult;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class DuckDbRepositoryImpl implements DuckDbRepository {

    private final Connection connection;
    public DuckDbRepositoryImpl() throws SQLException {
        this.connection = DriverManager.getConnection(
                "jdbc:duckdb:excel_dashboard_test.duckdb");
    }


    @Override
    public void cacheSheet(String tableName, Path filePath, String sheetName) throws SQLException {

        try (Statement statement = connection.createStatement()) {


            String sql = """ 
            CREATE OR REPLACE TABLE %s AS
            SELECT *
            FROM read_xlsx('%s', sheet='%s')
            """.formatted(
                    tableName,
                    filePath.toAbsolutePath().toString().replace("\\", "/"),
                    sheetName
            );

            statement.execute(sql);

        }
    }


    @Override
    public boolean tableExists(String tableName) throws SQLException {

        try (PreparedStatement preparedStatement = connection.prepareStatement("""
                 SELECT COUNT(*)
                 FROM information_schema.tables
                 WHERE table_name = ?
                 """)) {

            preparedStatement.setString(1, tableName);

            ResultSet resultSet = preparedStatement.executeQuery();

            resultSet.next();

            return resultSet.getInt(1) > 0;
        }
    }



    private boolean isStringColumn(
            String column,
            List<ColumnMetadataResponseDto> metadata) {

        for (ColumnMetadataResponseDto columnMetadata : metadata) {

            if (columnMetadata.getColumnName().equalsIgnoreCase(column)) {

                String dataType = columnMetadata.getDataType();

                return "VARCHAR".equalsIgnoreCase(dataType)
                        || "TEXT".equalsIgnoreCase(dataType);
            }
        }

        return false;
    }

    private String buildNormalizedComparison(String columnName) {

        return """
        LOWER(
            regexp_replace("%COLUMN%", '[^a-zA-Z0-9]', '', 'g')
        )
        LIKE
             CONCAT(
                  '%',
                  LOWER(regexp_replace(?, '[^a-zA-Z0-9]', '', 'g')),
                  '%'
                 )
        """.replace("%COLUMN%", columnName);
    }
    @Override
    public QueryResult query(String tableName, QueryRequest request,
            List<ColumnMetadataResponseDto> metadata) throws SQLException {

        List<Map<String, Object>> results = new ArrayList<>();

        StringBuilder sql = new StringBuilder();

        sql.append("SELECT * FROM ");
        sql.append(tableName);

        List<Object> parameters = new ArrayList<>();

        boolean hasWhereClause = false;

        // Search Filter
        if (request.getFilters() != null &&
                !request.getFilters().isEmpty()) {

            for (FilterRequest filter : request.getFilters()) {

                if (hasWhereClause) {
                    sql.append(" AND ");
                } else {
                    sql.append(" WHERE ");
                    hasWhereClause = true;
                }

                if (isStringColumn(filter.getColumn(), metadata)) {

                    sql.append(buildNormalizedComparison(filter.getColumn()));

                } else {

                    sql.append("\"");
                    sql.append(filter.getColumn());
                    sql.append("\" = ?");
                }

                parameters.add(filter.getValue());
            }
        }
        // Range Filters
        for (RangeFilterRequest filter : request.getRangeFilters()) {

            if (hasWhereClause) {
                sql.append(" AND ");
            } else {
                sql.append(" WHERE ");
                hasWhereClause = true;
            }

            sql.append("\"");
            sql.append(filter.getColumn());
            sql.append("\" ");

            if (filter.getMinValue() != null &&
                    filter.getMaxValue() != null) {

                sql.append("BETWEEN ? AND ?");

                parameters.add(filter.getMinValue());
                parameters.add(filter.getMaxValue());

            } else if (filter.getMinValue() != null) {

                sql.append(">= ?");

                parameters.add(filter.getMinValue());

            } else if (filter.getMaxValue() != null) {

                sql.append("<= ?");

                parameters.add(filter.getMaxValue());
            }
        }

        String countSql = sql.toString().replaceFirst(
                "SELECT \\*",
                "SELECT COUNT(*)"
        );

        // Sorting
        if (request.getSortColumn() != null &&
                !request.getSortColumn().isBlank() &&
                request.getSortDirection() != null) {
            sql.append(" ORDER BY \"");
            sql.append(request.getSortColumn());
            sql.append("\" ");
            sql.append(request.getSortDirection().name());
        }

        // Pagination
        sql.append(" LIMIT ?");
        sql.append(" OFFSET ?");

        Connection connection = this.connection;

            // counting the number rows

            long totalElements;
            System.out.println(sql);
            try (PreparedStatement countStatement =
                         connection.prepareStatement(countSql)) {

                for (int i = 0; i < parameters.size(); i++) {
                    countStatement.setObject(i + 1, parameters.get(i));
                }

                ResultSet countResult = countStatement.executeQuery();

                countResult.next();

                totalElements = countResult.getLong(1);
            }

            // Data fetching

            try (PreparedStatement preparedStatement =
                         connection.prepareStatement(sql.toString())) {

                int index = 1;

                for (Object parameter : parameters) {
                    preparedStatement.setObject(index++, parameter);
                }

                preparedStatement.setInt(index++, request.getSize());
                preparedStatement.setInt(index, request.getPage() * request.getSize());

                ResultSet resultSet = preparedStatement.executeQuery();

                ResultSetMetaData metaData = resultSet.getMetaData();

                while (resultSet.next()) {

                    Map<String, Object> row = new LinkedHashMap<>();

                    for (int i = 1; i <= metaData.getColumnCount(); i++) {

                        row.put(
                                metaData.getColumnName(i),
                                resultSet.getObject(i)
                        );
                    }

                    results.add(row);
                }
            }

            return new QueryResult(results, totalElements);

    }

    @Override
    public List<ColumnMetadataResponseDto> getColumnMetadata(
            String tableName) throws SQLException {

        List<ColumnMetadataResponseDto> columns = new ArrayList<>();

        try (Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery("""
                DESCRIBE %s
                """.formatted(tableName));

            while (resultSet.next()) {

                ColumnMetadataResponseDto column =
                        new ColumnMetadataResponseDto();

                column.setColumnName(
                        resultSet.getString("column_name")
                );

                column.setDataType(
                        resultSet.getString("column_type")
                );

                columns.add(column);
            }
        }

        return columns;
    }

    @Override
    public void dropTable(String tableName) throws SQLException {

        try (Statement statement = connection.createStatement()) {

            statement.execute("""
                DROP TABLE IF EXISTS %s
                """.formatted(tableName));
        }
    }
    @Override
    public void clearWorkspace() throws SQLException {

        try (Statement statement = connection.createStatement()) {

            ResultSet resultSet = statement.executeQuery("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_name LIKE 'file_%'
                """);

            while (resultSet.next()) {

                String tableName = resultSet.getString("table_name");

                statement.execute("""
                    DROP TABLE IF EXISTS %s
                    """.formatted(tableName));
            }
        }
    }
}
