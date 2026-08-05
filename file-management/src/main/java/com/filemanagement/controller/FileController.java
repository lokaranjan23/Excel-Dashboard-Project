package com.filemanagement.controller;

import com.filemanagement.response.ApiResponse;
import com.filemanagement.responseDto.CategoryResponseDto;
import com.filemanagement.responseDto.FileResponseDto;
import com.filemanagement.responseDto.FileSuggestionResponseDto;
import com.filemanagement.service.FileService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;


@RestController
@RequestMapping("api/v1/files")
public class FileController {

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<FileResponseDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam Long categoryId,
            @RequestParam String description) throws IOException {

        FileResponseDto uploadedFile =
                fileService.upload(file, categoryId, description);

        ApiResponse<FileResponseDto> response =
                new ApiResponse<>();

        response.setSuccess(true);
        response.setMessage("File uploaded successfully");
        response.setData(uploadedFile);
        response.setTimestamp(LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<FileResponseDto>>> getAllFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "uploadedAt") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search){

        Page<FileResponseDto> files =
                fileService.getAllFiles(
                        page,
                        size,
                        sortBy,
                        direction,categoryId,search);

        ApiResponse<Page<FileResponseDto>> response =
                new ApiResponse<>();

        response.setSuccess(true);
        response.setMessage("Files retrieved successfully");
        response.setData(files);
        response.setTimestamp(LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/category")
    public ResponseEntity<ApiResponse<List<CategoryResponseDto>>> getAllCategories() {

        List<CategoryResponseDto> categories =
                fileService.getAllCategories();

        ApiResponse<List<CategoryResponseDto>> response =
                new ApiResponse<>(
                        true,
                        "Categories retrieved successfully",
                        categories,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/category/suggest")
    public ResponseEntity<ApiResponse<CategoryResponseDto>> suggestCategory(
            @RequestParam String fileName) {

        CategoryResponseDto category = fileService.suggestCategory(fileName);

        ApiResponse<CategoryResponseDto> response =
                new ApiResponse<>(true,
                        "Category suggestion processed successfully",
                        category,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<FileSuggestionResponseDto>>> getSuggestions(
            @RequestParam String search) {

        List<FileSuggestionResponseDto> suggestions =
                fileService.getFileSuggestions(search);

        ApiResponse<List<FileSuggestionResponseDto>> response =
                new ApiResponse<>(
                        true,
                        "Suggestions retrieved successfully",
                        suggestions,
                        LocalDateTime.now()
                );

        return ResponseEntity.ok(response);
    }
}
