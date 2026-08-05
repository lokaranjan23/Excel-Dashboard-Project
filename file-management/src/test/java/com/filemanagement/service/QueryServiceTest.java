package com.filemanagement.service;

import com.filemanagement.entity.UploadedFile;
import com.filemanagement.enums.SortDirection;
import com.filemanagement.exception.ResourceNotFoundException;
import com.filemanagement.repository.UploadedFileRepository;
import com.filemanagement.requestDto.FilterRequest;
import com.filemanagement.requestDto.QueryRequest;
import com.filemanagement.requestDto.RangeFilterRequest;
import com.filemanagement.responseDto.ColumnMetadataResponseDto;
import com.filemanagement.responseDto.FileResponseDto;
import com.filemanagement.responseDto.QueryResponseDto;
import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class QueryServiceTest {

    @Autowired
    private DataSource duckDbDataSource;

    @Autowired
    private QueryService queryService;

    @Autowired
    private FileService fileService;

    @Value("${app.upload-dir}")
    private String uploadDirectory;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    private MockMultipartFile getTestFile(String fileName)
            throws IOException {
        ClassPathResource resource =
                new ClassPathResource(fileName);

        return new MockMultipartFile(
                "file",
                resource.getFilename(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                resource.getInputStream()
        );
    }


    private Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:duckdb:excel_dashboard_test.duckdb");
    }

    @Test
    void duckDb_ShouldReadExcelSheet() throws IOException, SQLException {
        MockMultipartFile file = getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile = fileService.upload(file, 1L, "Sales");
        System.out.println("1");
        UploadedFile savedFile = uploadedFileRepository.findById(uploadedFile.getId())
                .orElseThrow();
        System.out.println("2");
        Path filePath = Paths.get(uploadDirectory, savedFile.getStoredName());
        System.out.println("3");
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {


            System.out.println("4");
            ResultSet resultSet = statement.executeQuery("""
                    SELECT *
                    FROM read_xlsx('%s', sheet='Sales Data')
                    """
                    .formatted(filePath.toAbsolutePath().toString().replace("\\", "/")));
            System.out.println("5");
            // Assert
            assertTrue(resultSet.next());
        }

    }

    @Test
    void cacheSheet_ShouldCreateDuckDbTable() throws Exception {

        MockMultipartFile file = getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile = fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(uploadedFile.getId(), "Sales Data");

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            ResultSet resultSetCount = statement.executeQuery("""
                
                    SELECT count(*)
                FROM file_%d_Sales_Data
                """.formatted(uploadedFile.getId()));

            assertTrue((resultSetCount.next()));
            assertEquals(500, resultSetCount.getInt(1));

            ResultSet resultSet = statement.executeQuery(
                    """
                SELECT *
                FROM
                                    file_%d_Sales_Data
                LIMIT 1
                """.formatted(uploadedFile.getId()));

            assertTrue(resultSet.next());
            assertEquals("5001.0", resultSet.getString("Sale ID"));
            assertEquals("2026-06-01", resultSet.getString("Date"));
            assertEquals("1024.0", resultSet.getString("Employee ID"));
            assertEquals("Customer 19", resultSet.getString("Customer"));
            assertEquals("Keyboard", resultSet.getString("Product"));
            assertEquals("Accessories", resultSet.getString("Category"));
            assertEquals("2.0", resultSet.getString("Quantity"));
            assertEquals("45.0", resultSet.getString("Unit Price"));
            assertEquals("0.0", resultSet.getString("Discount"));
            assertEquals("90.0", resultSet.getString("Sales Amount"));
        }
    }

    @Test
    void cacheSheet_ShouldReplaceExistingCachedTable() throws IOException, SQLException {
        MockMultipartFile file = getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile = fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(uploadedFile.getId(), "Sales Data");
        queryService.cacheSheet(uploadedFile.getId(), "Sales Data");

        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {

            ResultSet resultSetCount = statement.executeQuery("""
                                    SELECT count(*)
                                    FROM file_%d_Sales_Data
                """.formatted(uploadedFile.getId()));

                assertTrue((resultSetCount.next()));
                assertEquals(500, resultSetCount.getInt(1));
                assertNotEquals(1000,resultSetCount.getInt(1));
       }
    }

    @Test
    void query_ShouldReturnAllRows_WhenNoFiltersAreApplied() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();
        request.setPage(0);
        request.setSize(500);

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());

        assertEquals(500, results.size());
        assertEquals(500, response.getTotalElements());
        assertEquals(1, response.getTotalPages());

        assertEquals(0, response.getPage());
        assertEquals(500, response.getSize());

        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }


    @Test
    void query_ShouldReturnSortedRowsInDescendingOrder() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setSortColumn("Sales Amount");
        request.setSortDirection(SortDirection.DESC);

        request.setPage(0);
        request.setSize(50);

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertNotNull(results);

        assertEquals(50, results.size());

        assertEquals(500, response.getTotalElements());
        assertEquals(10, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(50, response.getSize());
        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        double first =
                ((Number) results.getFirst().get("Sales Amount"))
                        .doubleValue();

        double second =
                ((Number) results.get(1).get("Sales Amount"))
                        .doubleValue();

        assertTrue(first >= second);
    }

    @Test
    void query_ShouldReturnFirstPage() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(50);

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertNotNull(results);
        assertEquals(50, results.size());

        assertEquals(500, response.getTotalElements());
        assertEquals(10, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(50, response.getSize());

        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        Map<String, Object> firstRow = results.getFirst();

        assertEquals("5001.0", firstRow.get("Sale ID").toString());
        assertEquals("Customer 19", firstRow.get("Customer"));
    }

    @Test
    void query_ShouldReturnSecondPage() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(1);
        request.setSize(50);

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertNotNull(results);
        assertEquals(50, results.size());

        assertEquals(500, response.getTotalElements());
        assertEquals(10, response.getTotalPages());
        assertEquals(1, response.getPage());
        assertEquals(50, response.getSize());

        assertFalse(response.isFirst());
        assertFalse(response.isLast());

        Map<String, Object> firstRow = results.getFirst();

        // Should not be the first record of the sheet
        assertNotEquals("5001.0", firstRow.get("Sale ID").toString());
    }

    @Test
    void query_ShouldReturnEmptyList_WhenPageExceedsAvailableData() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(10);
        request.setSize(50);

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertNotNull(results);
        assertTrue(results.isEmpty());

        assertEquals(500, response.getTotalElements());
        assertEquals(10, response.getTotalPages());
        assertEquals(10, response.getPage());
        assertEquals(50, response.getSize());

        assertFalse(response.isFirst());
        assertTrue(response.isLast());
    }


    @Test
    void query_ShouldReturnPaginatedSortedResults() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setSortColumn("Sales Amount");
        request.setSortDirection(SortDirection.DESC);

        request.setPage(0);
        request.setSize(20);

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertNotNull(results);
        assertEquals(20, results.size());

        assertEquals(500, response.getTotalElements());
        assertEquals(25, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());

        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        double first =
                ((Number) results.get(0).get("Sales Amount")).doubleValue();

        double second =
                ((Number) results.get(1).get("Sales Amount")).doubleValue();

        assertTrue(first >= second);
    }
    @Test
    void query_ShouldReturnFilteredRows() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(20);

        request.setFilters(List.of(
                new FilterRequest(
                        "Category",
                        "Accessories",
                        "VARCHAR"
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());

        assertEquals(20, results.size());
        assertEquals(217, response.getTotalElements());
        assertEquals(11, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());

        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        for (Map<String, Object> row : results) {

            assertEquals(
                    "Accessories",
                    row.get("Category")
            );
        }
    }
    @Test
    void query_ShouldReturnEmptyList_WhenFilterMatchesNothing() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(20);

        request.setFilters(List.of(
                new FilterRequest(
                        "Category",
                        "InvalidCategory",
                        "VARCHAR"
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertTrue(results.isEmpty());

        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());

        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }
    @Test
    void query_ShouldReturnRowsMatchingMultipleFilters() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(20);

        request.setFilters(List.of(
                new FilterRequest(
                        "Category",
                        "Accessories","VARCHAR"
                ),
                new FilterRequest(
                        "Product",
                        "Keyboard",
                        "VARCHAR"
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());

        assertEquals(20, results.size());

        assertEquals(109, response.getTotalElements());
        assertEquals(6, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());

        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        for (Map<String, Object> row : results) {

            assertEquals(
                    "Accessories",
                    row.get("Category")
            );

            assertEquals(
                    "Keyboard",
                    row.get("Product")
            );
        }
    }

    @Test
    void query_ShouldReturnFilteredSearchResults() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(20);

        request.setFilters(List.of(
                new FilterRequest(
                        "Category",
                        "Accessories",
                        "VARCHAR"
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());

        assertEquals(20, results.size());

        assertEquals(217, response.getTotalElements());
        assertEquals(11, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());

        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        for (Map<String, Object> row : results) {

            assertTrue(
                    row.get("Customer")
                            .toString()
                            .contains("Customer")
            );

            assertEquals(
                    "Accessories",
                    row.get("Category")
            );
        }
    }

    @Test
    void query_ShouldReturnFilteredSortedRows() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(20);

        request.setSortColumn("Sales Amount");
        request.setSortDirection(SortDirection.DESC);

        request.setFilters(List.of(
                new FilterRequest(
                        "Category",
                        "Accessories",
                        "VARCHAR"
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());

        assertEquals(20, results.size());
        assertEquals(217, response.getTotalElements());
        assertEquals(11, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(20, response.getSize());

        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        double first =
                ((Number) results.getFirst()
                        .get("Sales Amount"))
                        .doubleValue();

        double second =
                ((Number) results.get(1)
                        .get("Sales Amount"))
                        .doubleValue();

        assertTrue(first >= second);

        for (Map<String, Object> row : results) {

            assertEquals(
                    "Accessories",
                    row.get("Category")
            );
        }
    }
    @Test
    void query_ShouldReturnPaginatedFilteredRows() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(10);

        request.setFilters(List.of(
                new FilterRequest(
                        "Category",
                        "Accessories",
                        "VARCHAR"
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertEquals(10, results.size());

        assertEquals(217, response.getTotalElements());
        assertEquals(22, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());

        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        for (Map<String, Object> row : results) {

            assertEquals(
                    "Accessories",
                    row.get("Category")
            );
        }
    }

    @Test
    void query_ShouldReturnCorrectResult_WhenAllQueryOptionsAreApplied() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(10);

        request.setSortColumn("Sales Amount");
        request.setSortDirection(SortDirection.DESC);

        request.setFilters(List.of(
                new FilterRequest(
                        "Category",
                        "Accessories",
                        "VARCHAR"
                ),
                new FilterRequest(
                        "Product",
                        "Keyboard",
                        "VARCHAR"
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 10);

        assertEquals(10, response.getSize());
        assertEquals(0, response.getPage());
        assertEquals(109, response.getTotalElements());
        assertEquals(11, response.getTotalPages());

        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        double previous = Double.MAX_VALUE;

        for (Map<String, Object> row : results) {

            assertTrue(
                    row.get("Customer")
                            .toString()
                            .contains("Customer")
            );

            assertEquals(
                    "Accessories",
                    row.get("Category")
            );

            assertEquals(
                    "Keyboard",
                    row.get("Product")
            );

            double current =
                    ((Number) row.get("Sales Amount"))
                            .doubleValue();

            assertTrue(current <= previous);

            previous = current;
        }
    }
    @Test
    void query_ShouldReturnRowsWithinRange() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(500);

        request.setRangeFilters(List.of(
                new RangeFilterRequest(
                        "Sales Amount",
                        100,
                        500
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());

        assertEquals(0, response.getPage());
        assertEquals(500, response.getSize());
        assertTrue(response.getTotalElements() > 0);
        assertEquals(1, response.getTotalPages());
        assertTrue(response.isFirst());
        assertTrue(response.isLast());

        for (Map<String, Object> row : results) {

            double salesAmount =
                    ((Number) row.get("Sales Amount"))
                            .doubleValue();

            assertTrue(salesAmount >= 100);
            assertTrue(salesAmount <= 500);
        }
    }

    @Test
    void query_ShouldReturnEmptyList_WhenRangeMatchesNothing() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(500);

        request.setRangeFilters(List.of(
                new RangeFilterRequest(
                        "Sales Amount",
                        100000,
                        200000
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertNotNull(results);
        assertTrue(results.isEmpty());

        assertEquals(0, response.getTotalElements());
        assertEquals(0, response.getTotalPages());
        assertEquals(0, response.getPage());
        assertEquals(500, response.getSize());

        assertTrue(response.isFirst());
        assertTrue(response.isLast());
    }
    @Test
    void query_ShouldReturnRowsMatchingMultipleRanges() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(500);

        request.setRangeFilters(List.of(
                new RangeFilterRequest(
                        "Sales Amount",
                        100,
                        500
                ),
                new RangeFilterRequest(
                        "Quantity",
                        2,
                        5
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());

        assertEquals(0, response.getPage());
        assertEquals(500, response.getSize());
        assertTrue(response.getTotalElements() > 0);
        assertEquals(1, response.getTotalPages());

        assertTrue(response.isFirst());
        assertTrue(response.isLast());

        for (Map<String, Object> row : results) {

            double salesAmount =
                    ((Number) row.get("Sales Amount"))
                            .doubleValue();

            double quantity =
                    ((Number) row.get("Quantity"))
                            .doubleValue();

            assertTrue(salesAmount >= 100);
            assertTrue(salesAmount <= 500);

            assertTrue(quantity >= 2);
            assertTrue(quantity <= 5);
        }
    }

    @Test
    void query_ShouldReturnRangeFilteredExactFilterResults() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(500);

        request.setFilters(List.of(
                new FilterRequest(
                        "Category",
                        "Accessories",
                "VARCHAR")
        ));

        request.setRangeFilters(List.of(
                new RangeFilterRequest(
                        "Sales Amount",
                        100,
                        500
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());

        assertEquals(0, response.getPage());
        assertEquals(500, response.getSize());
        assertTrue(response.getTotalElements() > 0);
        assertEquals(1, response.getTotalPages());

        assertTrue(response.isFirst());
        assertTrue(response.isLast());

        for (Map<String, Object> row : results) {

            assertEquals(
                    "Accessories",
                    row.get("Category")
            );

            double salesAmount =
                    ((Number) row.get("Sales Amount"))
                            .doubleValue();

            assertTrue(salesAmount >= 100);
            assertTrue(salesAmount <= 500);
        }
    }

    @Test
    void query_ShouldReturnRangeFilteredSortedRows() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(500);

        request.setSortColumn("Sales Amount");
        request.setSortDirection(SortDirection.DESC);

        request.setRangeFilters(List.of(
                new RangeFilterRequest(
                        "Sales Amount",
                        100,
                        500
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());

        assertEquals(0, response.getPage());
        assertEquals(500, response.getSize());
        assertTrue(response.getTotalElements() > 0);
        assertEquals(1, response.getTotalPages());

        assertTrue(response.isFirst());
        assertTrue(response.isLast());

        double previous = Double.MAX_VALUE;

        for (Map<String, Object> row : results) {

            double current =
                    ((Number) row.get("Sales Amount"))
                            .doubleValue();

            assertTrue(current >= 100);
            assertTrue(current <= 500);

            assertTrue(current <= previous);

            previous = current;
        }
    }
    @Test
    void query_ShouldReturnPaginatedRangeFilteredRows() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(10);

        request.setRangeFilters(List.of(
                new RangeFilterRequest(
                        "Sales Amount",
                        100,
                        500
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertEquals(10, results.size());

        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertTrue(response.getTotalElements() > 0);
        assertTrue(response.getTotalPages() >= 1);

        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        for (Map<String, Object> row : results) {

            double salesAmount =
                    ((Number) row.get("Sales Amount"))
                            .doubleValue();

            assertTrue(salesAmount >= 100);
            assertTrue(salesAmount <= 500);
        }
    }
    @Test
    void query_ShouldReturnCorrectResult_WhenAllQueryOptionsIncludingRangeAreApplied() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        QueryRequest request = new QueryRequest();

        request.setPage(0);
        request.setSize(10);

        request.setSortColumn("Sales Amount");
        request.setSortDirection(SortDirection.DESC);

        request.setFilters(List.of(new FilterRequest("Category",
                        "Accessories","VARCHAR"
                )
        ));

        request.setRangeFilters(List.of(
                new RangeFilterRequest(
                        "Sales Amount",
                        100,
                        500
                )
        ));

        QueryResponseDto response =
                queryService.query(
                        uploadedFile.getId(),
                        "Sales Data",
                        request
                );

        assertNotNull(response);

        List<Map<String, Object>> results =
                response.getContent();

        assertFalse(results.isEmpty());
        assertTrue(results.size() <= 10);

        assertEquals(0, response.getPage());
        assertEquals(10, response.getSize());
        assertTrue(response.getTotalElements() > 0);
        assertTrue(response.getTotalPages() >= 1);

        assertTrue(response.isFirst());
        assertFalse(response.isLast());

        double previous = Double.MAX_VALUE;

        for (Map<String, Object> row : results) {

            assertTrue(
                    row.get("Customer")
                            .toString()
                            .contains("Customer")
            );

            assertEquals(
                    "Accessories",
                    row.get("Category")
            );

            double salesAmount =
                    ((Number) row.get("Sales Amount"))
                            .doubleValue();

            assertTrue(salesAmount >= 100);
            assertTrue(salesAmount <= 500);

            assertTrue(salesAmount <= previous);

            previous = salesAmount;
        }
    }

    @Test
    void getColumnMetadata_ShouldReturnAllColumns() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        List<ColumnMetadataResponseDto> columns =
                queryService.getColumnMetadata(
                        uploadedFile.getId(),
                        "Sales Data"
                );

        assertNotNull(columns);

        assertEquals(10, columns.size());

        assertEquals("Sale ID", columns.get(0).getColumnName());
        assertEquals("Date", columns.get(1).getColumnName());
        assertEquals("Employee ID", columns.get(2).getColumnName());
        assertEquals("Customer", columns.get(3).getColumnName());
        assertEquals("Product", columns.get(4).getColumnName());
        assertEquals("Category", columns.get(5).getColumnName());
        assertEquals("Quantity", columns.get(6).getColumnName());
        assertEquals("Unit Price", columns.get(7).getColumnName());
        assertEquals("Discount", columns.get(8).getColumnName());
        assertEquals("Sales Amount", columns.get(9).getColumnName());
    }

    @Test
    void getColumnMetadata_ShouldReturnCorrectDataTypes() throws Exception {

        MockMultipartFile file =
                getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile =
                fileService.upload(file, 1L, "Sales");

        queryService.cacheSheet(
                uploadedFile.getId(),
                "Sales Data"
        );

        List<ColumnMetadataResponseDto> columns =
                queryService.getColumnMetadata(
                        uploadedFile.getId(),
                        "Sales Data"
                );

        assertNotNull(columns);

        assertEquals("DOUBLE", columns.get(0).getDataType());
        assertEquals("DATE", columns.get(1).getDataType());
        assertEquals("DOUBLE", columns.get(2).getDataType());
        assertEquals("VARCHAR", columns.get(3).getDataType());
        assertEquals("VARCHAR", columns.get(4).getDataType());
        assertEquals("VARCHAR", columns.get(5).getDataType());
        assertEquals("DOUBLE", columns.get(6).getDataType());
        assertEquals("DOUBLE", columns.get(7).getDataType());
        assertEquals("DOUBLE", columns.get(8).getDataType());
        assertEquals("DOUBLE", columns.get(9).getDataType());
    }

    @Test
    void getColumnMetadata_ShouldThrowException_WhenTableDoesNotExist() {

        assertThrows(ResourceNotFoundException.class, () -> queryService.getColumnMetadata(
                        999L,
                "Sales Data"));
    }


}
