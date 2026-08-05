package com.filemanagement.repository;

import com.filemanagement.entity.UploadedFile;
import com.filemanagement.enums.FileStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.List;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile,Long> {

    Page<UploadedFile> findByCategoryId(
            Long categoryId,
            Pageable pageable);

    Page<UploadedFile> findByOriginalNameContainingIgnoreCase(
            String search,
            Pageable pageable);

    Page<UploadedFile> findByCategoryIdAndOriginalNameContainingIgnoreCase(
            Long categoryId,
            String search,
            Pageable pageable);

    boolean existsByOriginalNameAndStatus(
            String originalName,
            FileStatus status);

    List<UploadedFile> findTop10ByOriginalNameContainingIgnoreCase(String search);
}
