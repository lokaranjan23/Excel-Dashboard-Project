package com.filemanagement.responseDto;

import com.filemanagement.enums.FileStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FileResponseDto {

    private Long id;

    private String originalName;

    private String description;

    private Long categoryId;

    private String categoryName;

    private Long fileSize;

    private LocalDateTime uploadedAt;

    private FileStatus status;
}
