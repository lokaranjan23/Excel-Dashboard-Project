package com.filemanagement.service;

import com.filemanagement.entity.Category;
import com.filemanagement.entity.UploadedFile;
import com.filemanagement.enums.FileStatus;
import com.filemanagement.exception.DuplicateFileException;
import com.filemanagement.exception.InvalidFileException;
import com.filemanagement.exception.InvalidSortFieldException;
import com.filemanagement.exception.ResourceNotFoundException;
import com.filemanagement.repository.CategoryRepository;
import com.filemanagement.repository.UploadedFileRepository;
import com.filemanagement.requestDto.QueryRequest;
import com.filemanagement.responseDto.CategoryResponseDto;
import com.filemanagement.responseDto.FileResponseDto;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;


@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class FileServiceTest {

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Autowired
    private FileService fileService;

    @Value("${app.upload-dir}")
    private String uploadDirectory;

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


    @Test
    void upload_ShouldThrowException_WhenCategoryDoesNotExist() throws IOException {
        String fileName="test-files/Sales_Test_Dataset.xlsx";
        MockMultipartFile file=getTestFile(fileName);

        ResourceNotFoundException exception=
                assertThrows(ResourceNotFoundException.class,()->
                fileService.upload(file,100L,
                        "July sales document"));

        assertEquals("Category not found", exception.getMessage());
        assertEquals(0,uploadedFileRepository.count());
    }

    @Test
    void upload_ShouldSaveFile_WhenRequestIsValid() throws IOException{
        MockMultipartFile file=getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto response =
                fileService.upload(file, 1L, "July Sales Report");
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Sales_Test_Dataset.xlsx", response.getOriginalName());
        assertEquals("SALES", response.getCategoryName());
        assertEquals(FileStatus.ACTIVE, response.getStatus());
        assertEquals("July Sales Report", response.getDescription());

        assertEquals(1,uploadedFileRepository.count());

        UploadedFile savedFile=uploadedFileRepository.findById(response.getId())
                .orElseThrow();
        assertEquals(file.getOriginalFilename(), savedFile.getOriginalName());
        assertNotEquals(savedFile.getOriginalName(),savedFile.getStoredName());
        assertEquals("July Sales Report", savedFile.getDescription());
        assertEquals(FileStatus.ACTIVE, savedFile.getStatus());
        assertEquals(1L, savedFile.getCategory().getId());
        assertEquals("SALES", savedFile.getCategory().getName());
        assertEquals(file.getSize(), savedFile.getFileSize());
        assertNotNull(savedFile.getUploadedAt());
        assertNotNull(savedFile.getStoredName());
        assertTrue(savedFile.getStoredName().endsWith(".xlsx"));
    }

    @Test
    void upload_ShouldThrowException_WhenFileIsEmpty(){
        MockMultipartFile file=new MockMultipartFile(
                "file",
                "Sales_Test_Dataset.xlsx",
                "application/vnd.openxmlformats-" +
                        "officedocument.spreadsheetml.sheet",
                new byte[0]);
        assertThrows(InvalidFileException.class, ()->
                fileService.upload(file,1L,
                        "July sales document"));
        assertEquals(0,uploadedFileRepository.count());

    }

    @Test
    void upload_ShouldThrowException_WhenFileIsNotInExcelFormat(){
        MockMultipartFile file=new MockMultipartFile(
                "file",
                "sampleFile.pdf",
                "application/pdf",
                "dummy data".getBytes());

        InvalidFileException exception= assertThrows(InvalidFileException.class,()->
                fileService.upload(file,1L,
                        "July Sales data"));
        assertEquals("File format invalid",exception.getMessage());
        assertEquals(0,uploadedFileRepository.count());

    }

    @Test
    void upload_ShouldGenerateUniqueStoredFileName() throws IOException {

        MockMultipartFile file=getTestFile("test-files/Sales_Test_Dataset.xlsx");


        FileResponseDto response = fileService.upload(file,
                1L, "July sales data");
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals(1L, response.getCategoryId());
        assertEquals("July sales data",response.getDescription());
        assertEquals(file.getSize(),response.getFileSize());
        assertEquals(file.getOriginalFilename(),response.getOriginalName());
        assertEquals(FileStatus.ACTIVE,response.getStatus());

        UploadedFile uploadedFile = uploadedFileRepository.findById(response.getId())
                .orElseThrow();
        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf('.'));

        assertEquals(originalName,uploadedFile.getOriginalName());
        assertNotEquals(uploadedFile.getOriginalName(),uploadedFile.getStoredName());
        assertTrue(uploadedFile.getStoredName().endsWith(extension));
        assertNotNull(uploadedFile.getStoredName());
        assertFalse(uploadedFile.getStoredName().isBlank());

        assertEquals(1,uploadedFileRepository.count());
    }

    @Test
    void upload_ShouldStoreFileOnDisk() throws IOException {
        MultipartFile file=getTestFile("test-files/Sales_Test_Dataset.xlsx");

        FileResponseDto response =
                fileService.upload(file, 1L, "July sales data");

        UploadedFile uploadedFile = uploadedFileRepository.findById(response.getId())
                        .orElseThrow();

        Path storedFile = Paths.get(uploadDirectory, uploadedFile.getStoredName());

        assertTrue(Files.exists(storedFile));

    }

    @Test
    void upload_ShouldThrowException_WhenOriginalFileNameAlreadyExists() throws IOException {
        MockMultipartFile sales = getTestFile("test-files/Sales_Data.xlsx");

        MockMultipartFile inventory = getTestFile("test-files/Sales_Data.xlsx");

        fileService.upload(sales, 1L, "Sales Report");
        DuplicateFileException exception = assertThrows(DuplicateFileException.class, () ->
                fileService.upload(inventory, 2L, "Sales Report"));
        assertEquals("A file with the same name already exists.",exception.getMessage());

    }

    @Test
    void getAllFiles_ShouldReturnFilesOfSelectedCategory() throws IOException {

        MockMultipartFile sales =
                getTestFile("test-files/Sales_Data.xlsx");

        MockMultipartFile inventory =
                getTestFile("test-files/Inventory_Data.xlsx");

        MockMultipartFile employee =
                getTestFile("test-files/Employee_Data.xlsx");

        fileService.upload(sales, 1L, "Sales Report");
        fileService.upload(inventory, 2L, "Inventory Report");
        fileService.upload(employee, 3L, "Employee Report");

        Page<FileResponseDto> result =
                fileService.getAllFiles(
                        0,
                        10,
                        "uploadedAt",
                        Sort.Direction.ASC,
                        1L,
                        null
                );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        FileResponseDto file = result.getContent().getFirst();

        assertEquals("Sales_Data.xlsx", file.getOriginalName());
        assertEquals(1L, file.getCategoryId());
        assertEquals("Sales", file.getCategoryName());
    }

    @Test
    void getAllFiles_ShouldReturnFilesMatchingSearch() throws IOException {

        MockMultipartFile sales =
                getTestFile("test-files/Sales_Data.xlsx");

        MockMultipartFile inventory =
                getTestFile("test-files/Inventory_Data.xlsx");

        MockMultipartFile employee =
                getTestFile("test-files/Employee_Data.xlsx");

        fileService.upload(sales, 1L, "Sales Report");
        fileService.upload(inventory, 2L, "Inventory Report");
        fileService.upload(employee, 3L, "Employee Report");

        Page<FileResponseDto> result =
                fileService.getAllFiles(
                        0,
                        10,
                        "uploadedAt",
                        Sort.Direction.ASC,
                        null,
                        "sales"
                );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        FileResponseDto file = result.getContent().getFirst();

        assertEquals("Sales_Data.xlsx", file.getOriginalName());
        assertEquals(1L, file.getCategoryId());
    }

    @Test
    void getAllFiles_ShouldReturnFilesMatchingCategoryAndSearch() throws IOException {

        MockMultipartFile sales =
                getTestFile("test-files/Sales_Data.xlsx");

        MockMultipartFile inventory =
                getTestFile("test-files/Inventory_Data.xlsx");

        MockMultipartFile employee =
                getTestFile("test-files/Employee_Data.xlsx");

        fileService.upload(sales, 1L, "Sales Report");
        fileService.upload(inventory, 2L, "Inventory Report");
        fileService.upload(employee, 3L, "Employee Report");

        Page<FileResponseDto> result =
                fileService.getAllFiles(
                        0,
                        10,
                        "uploadedAt",
                        Sort.Direction.ASC,
                        1L,
                        "sales"
                );

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());

        FileResponseDto file = result.getContent().getFirst();

        assertEquals("Sales_Data.xlsx", file.getOriginalName());
        assertEquals(1L, file.getCategoryId());
    }

    @Test
    void getAllFiles_ShouldReturnEmptyPage_WhenCategoryAndSearchDoNotMatch() throws IOException {

        MockMultipartFile sales =
                getTestFile("test-files/Sales_Data.xlsx");

        MockMultipartFile inventory =
                getTestFile("test-files/Inventory_Data.xlsx");

        fileService.upload(sales, 1L, "Sales Report");
        fileService.upload(inventory, 2L, "Inventory Report");

        Page<FileResponseDto> result =
                fileService.getAllFiles(
                        0,
                        10,
                        "uploadedAt",
                        Sort.Direction.ASC,
                        1L,
                        "inventory"
                );

        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getContent().size());
        assertEquals(0, result.getTotalElements());
    }

    @Test
    void getAllFiles_ShouldSortByOriginalNameAtAscending() throws IOException {
        MockMultipartFile file1 = getTestFile("test-files/Sales_Data.xlsx");
        MockMultipartFile file2 = getTestFile("test-files/Inventory_Data.xlsx");
        MockMultipartFile file3 = getTestFile("test-files/Employee_Data.xlsx");

        FileResponseDto uploadedFile1 = fileService.upload(file1, 1L, "Sales Report");
        FileResponseDto uploadedFile2 = fileService.upload(file2, 2L, "Inventory Report");
        FileResponseDto uploadedFile3 = fileService.upload(file3, 3L, "Employee Report");

        Page<FileResponseDto> result = fileService.getAllFiles(0, 2,
                "originalName", Sort.Direction.ASC,null,null);

        assertNotNull(result);
        assertEquals(uploadedFile3.getOriginalName(),result.getContent().getFirst().getOriginalName());
        assertEquals(uploadedFile2.getOriginalName(),result.getContent().getLast().getOriginalName());

    }

    @Test
    void getAllFiles_ShouldThrowException_WhenSortFieldIsInvalid() throws IOException {
        MockMultipartFile file = getTestFile("test-files/Sales_Data.xlsx");

        fileService.upload(file, 1L, "Sales Report");

        InvalidSortFieldException exception = assertThrows(InvalidSortFieldException.class,
                        () -> fileService.getAllFiles(0, 10, "abc",
                                Sort.Direction.ASC,null,null));
        assertEquals("Invalid sort field", exception.getMessage());
    }

    @Test
    void suggestCategory_ShouldReturnMatchingCategory_WhenFileNameContainsCategoryName() {

        String fileName = "Sales_Report.xlsx";

        CategoryResponseDto category = fileService.suggestCategory(fileName);

        assertNotNull(category);

        assertEquals("SALES", category.getName());
    }

    @Test
    void suggestCategory_ShouldReturnNull_WhenFileNameIsNull() {

        CategoryResponseDto result =
                fileService.suggestCategory(null);

        assertNull(result);
    }

    @Test
    void suggestCategory_ShouldReturnNull_WhenFileNameIsBlank() {

        CategoryResponseDto result =
                fileService.suggestCategory("   ");

        assertNull(result);
    }

    @Test
    void suggestCategory_ShouldReturnNull_WhenNoCategoryMatches() {

        CategoryResponseDto result =
                fileService.suggestCategory("January_Report.xlsx");

        assertNull(result);
    }

    @Test
    void suggestCategory_ShouldMatch_WhenCategoryAppearsAnywhereInFileName() {

        CategoryResponseDto result = fileService.suggestCategory(
                        "Monthly_Sales_Report.xlsx");

        assertNotNull(result);

        assertEquals("SALES", result.getName());
    }





}
