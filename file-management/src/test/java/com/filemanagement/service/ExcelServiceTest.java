package com.filemanagement.service;

import com.filemanagement.entity.UploadedFile;
import com.filemanagement.exception.InvalidFileException;
import com.filemanagement.exception.ResourceNotFoundException;
import com.filemanagement.exception.StoredFileNotFoundException;
import com.filemanagement.repository.UploadedFileRepository;
import com.filemanagement.responseDto.FileResponseDto;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ExcelServiceTest {

    @Autowired
    private ExcelService excelService;

    @Autowired
    private FileService fileService;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Value("${app.upload-dir}")
    private String uploadDirectory;

    private MockMultipartFile getTestFile(String fileName) throws IOException {
        ClassPathResource resource = new ClassPathResource(fileName);

        return new MockMultipartFile(
                "file",
                resource.getFilename(),
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                resource.getInputStream()
        );
    }


    @Test
    void getSheetNames_ShouldReturnAllSheetNames() throws IOException, StoredFileNotFoundException {
        MockMultipartFile file = getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile = fileService.upload(file, 1L,
                "July sales report");

        List<String> sheetNames = excelService.getSheetNames(uploadedFile.getId());

        assertNotNull(sheetNames);
        assertEquals(3,sheetNames.size());
        assertEquals(3, sheetNames.size());

        assertTrue(sheetNames.contains("Sales Data"));
        assertTrue(sheetNames.contains("Employee Details"));
        assertTrue(sheetNames.contains("Inventory Data"));
    }

    @Test
    void getSheetNames_ShouldThrowException_WhenFileDoesNotExist() throws IOException {
        ResourceNotFoundException exception = assertThrows(
                        ResourceNotFoundException.class,
                        () -> excelService.getSheetNames(100L));
        assertEquals("File not found", exception.getMessage());

    }

    @Test
    void getSheetNames_ShouldThrowException_WhenStoredFileDoesNotExist() throws IOException{
        MockMultipartFile file = getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto uploadedFile = fileService.upload(file, 1L,
                "July sales report");

        UploadedFile savedFile = uploadedFileRepository.findById(uploadedFile.getId())
                        .orElseThrow();
        Path storedFile= Paths.get(uploadDirectory,savedFile.getStoredName());
        assertTrue(Files.exists(storedFile));
        Files.delete(storedFile);

        StoredFileNotFoundException exception=assertThrows(
                StoredFileNotFoundException.class,()-> excelService.getSheetNames(uploadedFile.getId()));
        assertEquals("Stored file not found on disk",exception.getMessage());

    }

    @Test
    void getSheetNames_ShouldThrowException_WhenWorkbookIsCorrupted() throws IOException {
        MockMultipartFile file = getTestFile("test-files/Corrupted.xlsx");

        FileResponseDto uploadedFile = fileService.upload(file, 1L,
                        "Corrupted workbook");

        InvalidFileException exception = assertThrows(InvalidFileException.class,
                () -> excelService.getSheetNames(uploadedFile.getId())
        );

        assertEquals(
                "Invalid or corrupted Excel file",
                exception.getMessage()
        );
    }

}
