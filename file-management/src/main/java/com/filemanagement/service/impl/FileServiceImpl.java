package com.filemanagement.service.impl;

import com.filemanagement.entity.Category;
import com.filemanagement.entity.UploadedFile;
import com.filemanagement.enums.FileStatus;
import com.filemanagement.exception.DuplicateFileException;
import com.filemanagement.exception.InvalidFileException;
import com.filemanagement.exception.InvalidSortFieldException;
import com.filemanagement.exception.ResourceNotFoundException;
import com.filemanagement.repository.CategoryRepository;
import com.filemanagement.repository.UploadedFileRepository;
import com.filemanagement.responseDto.CategoryResponseDto;
import com.filemanagement.responseDto.FileResponseDto;
import com.filemanagement.responseDto.FileSuggestionResponseDto;
import com.filemanagement.service.FileService;
import com.filemanagement.util.StringUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.print.attribute.Attribute;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static com.filemanagement.util.StringUtil.toTitleCase;

@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private final CategoryRepository categoryRepository;
    private final UploadedFileRepository uploadedFileRepository;

    @Value("${app.upload-dir}")
    private String uploadDirectory;

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("uploadedAt", "originalName",
            "status", "fileSize");

    @Override
    public FileResponseDto upload(MultipartFile file, Long categoryId,
                                  String description) throws IOException {
        if(!Objects.equals(file.getContentType(), "application/vnd.openxmlformats-" +
                "officedocument.spreadsheetml.sheet")){
            throw new InvalidFileException("File format invalid");
        }
        if(file.isEmpty()){
            throw new InvalidFileException("File cannot be empty");
        }
        if(uploadedFileRepository.existsByOriginalNameAndStatus(file.getOriginalFilename()
                ,FileStatus.ACTIVE)){
            throw new DuplicateFileException("A file with the same name already exists.");
        }
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Category not found"));
        UploadedFile uploadedFile = new UploadedFile();
        String originalName=file.getOriginalFilename();
        uploadedFile.setOriginalName(originalName);

        String extension= Objects.requireNonNull(originalName).substring(originalName.lastIndexOf("."));

        uploadedFile.setStoredName(generateStoredFileName(originalName));
        uploadedFile.setFileSize(file.getSize());
        uploadedFile.setCategory(category);
        uploadedFile.setDescription(description);
        uploadedFile.setStatus(FileStatus.ACTIVE);

        UploadedFile savedFile = uploadedFileRepository.save(uploadedFile);

        Path uploadPath= Paths.get(uploadDirectory);

        Files.createDirectories(uploadPath);
        Path destination = uploadPath.resolve(uploadedFile.getStoredName());
        Files.copy(file.getInputStream(), destination,
                StandardCopyOption.REPLACE_EXISTING);
        FileResponseDto response = new FileResponseDto();

        response.setId(savedFile.getId());
        response.setOriginalName(savedFile.getOriginalName());
        response.setDescription(savedFile.getDescription());
        response.setCategoryId(savedFile.getCategory().getId());
        response.setCategoryName(savedFile.getCategory().getName());
        response.setFileSize(savedFile.getFileSize());
        response.setUploadedAt(savedFile.getUploadedAt());
        response.setStatus(savedFile.getStatus());

        return response;
    }

    private String generateStoredFileName(String originalFileName) {

        int lastDotIndex = originalFileName.lastIndexOf('.');

        String fileName = lastDotIndex != -1
                ? originalFileName.substring(0, lastDotIndex)
                : originalFileName;

        String extension = lastDotIndex != -1
                ? originalFileName.substring(lastDotIndex)
                : "";

        String sanitizedName = fileName
                .replaceAll("[\\\\/:*?\"<>|]", "")
                .replaceAll("\\s+", "_");

        return sanitizedName + "_" + UUID.randomUUID() + extension;
    }

    @Override
    public Page<FileResponseDto> getAllFiles(int page, int size, String sortBy, Sort.Direction direction,
    Long categoryId, String search) {

        if(!ALLOWED_SORT_FIELDS.contains(sortBy)){
            throw new InvalidSortFieldException("Invalid sort field");
        }
        Pageable pageable = PageRequest.of(page, size,
                Sort.by(direction, sortBy));
        Page<UploadedFile> uploadedFiles;

        boolean hasCategory = categoryId != null;
        boolean hasSearch =
                search != null && !search.isBlank();

        if (hasCategory && hasSearch) {

            uploadedFiles =
                    uploadedFileRepository
                            .findByCategoryIdAndOriginalNameContainingIgnoreCase(
                                    categoryId,
                                    search,
                                    pageable);

        } else if (hasCategory) {

            uploadedFiles =
                    uploadedFileRepository
                            .findByCategoryId(
                                    categoryId,
                                    pageable);

        } else if (hasSearch) {

            uploadedFiles =
                    uploadedFileRepository
                            .findByOriginalNameContainingIgnoreCase(
                                    search,
                                    pageable);

        } else {

            uploadedFiles =
                    uploadedFileRepository.findAll(pageable);
        }
        Page<FileResponseDto> response = uploadedFiles.map(uploadedFile -> {
                    FileResponseDto dto = new FileResponseDto();

                    dto.setId(uploadedFile.getId());
                    dto.setOriginalName(uploadedFile.getOriginalName());
                    dto.setDescription(uploadedFile.getDescription());
                    dto.setCategoryId(uploadedFile.getCategory().getId());
                    dto.setCategoryName(toTitleCase(
                                    uploadedFile.getCategory().getName()));
                    dto.setFileSize(uploadedFile.getFileSize());
                    dto.setUploadedAt(uploadedFile.getUploadedAt());
                    dto.setStatus(uploadedFile.getStatus());
                    return dto;
                });
        return response;
    }

    @Override
    public List<CategoryResponseDto> getAllCategories() {

        List<Category> categories = categoryRepository.findAll(
                Sort.by(Sort.Direction.ASC, "name"));

        List<CategoryResponseDto> result=new ArrayList<>();
        for(Category category:categories){
            CategoryResponseDto dto= new CategoryResponseDto();
            dto.setId(category.getId());
            dto.setName(category.getName());
            result.add(dto);
        }
        return result;
    }

    @Override
    public CategoryResponseDto suggestCategory(String fileName) {
        if(fileName==null || fileName.isBlank()){
            return null;
        }
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC,
                "name"));

        String normalizedFileName=fileName.toLowerCase();

        for(Category category:categories){
            String[] keywords = category.getKeywords().split(",");

            for(String keyword:keywords) {
                if (normalizedFileName.contains(keyword.trim().toLowerCase())) {
                    CategoryResponseDto dto = new CategoryResponseDto();
                    dto.setId(category.getId());
                    dto.setName(category.getName());
                    return dto;
                }
            }

        }
        return null;
    }

    @Override
    public List<FileSuggestionResponseDto> getFileSuggestions(String search) {
        List<UploadedFile> files = uploadedFileRepository
                .findTop10ByOriginalNameContainingIgnoreCase(search);
        List<FileSuggestionResponseDto> response= new ArrayList<>();

        for(UploadedFile file:files){
            FileSuggestionResponseDto dto= new FileSuggestionResponseDto();
            dto.setId(file.getId());
            dto.setOriginalName(file.getOriginalName());
            response.add(dto);
        }
        return response;
    }


}
