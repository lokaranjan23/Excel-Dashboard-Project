package com.filemanagement.service.impl;

import com.filemanagement.entity.UploadedFile;
import com.filemanagement.exception.InvalidFileException;
import com.filemanagement.exception.InvalidSheetException;
import com.filemanagement.exception.ResourceNotFoundException;
import com.filemanagement.exception.StoredFileNotFoundException;
import com.filemanagement.repository.UploadedFileRepository;
import com.filemanagement.service.ExcelService;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ExcelServiceImpl implements ExcelService {

    @Value("${app.upload-dir}")
    private String uploadDirectory;

    private final UploadedFileRepository uploadedFileRepository;

    public ExcelServiceImpl(UploadedFileRepository uploadedFileRepository) {
        this.uploadedFileRepository = uploadedFileRepository;
    }

    private Path getFilePath(Long fileId) {

        UploadedFile uploadedFile = uploadedFileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found"));

        Path filePath = Paths.get(uploadDirectory, uploadedFile.getStoredName());

        if (!Files.exists(filePath)) {
            throw new StoredFileNotFoundException("Stored file not found on disk");
        }

        return filePath;
    }

    @Override
    public List<String> getSheetNames(Long fileId) {
        Path filePath = getFilePath(fileId);
        try (Workbook workbook = WorkbookFactory.create(filePath.toFile())) {
            List<String> sheetNames = new ArrayList<>();

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                sheetNames.add(workbook.getSheetName(i));
            }

            return sheetNames;

        } catch (FileNotFoundException ex) {
            throw new InvalidFileException(
                    "The Excel file is currently open in another application. Please close it and try again."
            );
        }
        catch (Exception e) {
            e.printStackTrace();
            throw new InvalidFileException("Invalid or corrupted Excel file", e);
        }
    }



}
