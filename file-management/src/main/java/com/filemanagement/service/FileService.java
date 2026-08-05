package com.filemanagement.service;

import com.filemanagement.responseDto.CategoryResponseDto;
import com.filemanagement.responseDto.FileResponseDto;
import com.filemanagement.responseDto.FileSuggestionResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@Service
public interface FileService {

    FileResponseDto upload(MultipartFile file, Long categoryId, String description) throws IOException;
    Page<FileResponseDto> getAllFiles(int page, int size, String sortBy, Sort.Direction direction,
            Long categoryId, String search);
    List<CategoryResponseDto> getAllCategories();

    CategoryResponseDto suggestCategory(String fileName);

    List<FileSuggestionResponseDto> getFileSuggestions(String search);
}
