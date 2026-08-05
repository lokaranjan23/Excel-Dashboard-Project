package com.filemanagement.entity;

import com.filemanagement.enums.FileStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name="uploaded_files")
@Getter
@Setter
public class UploadedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name="original_name",nullable=false,length = 255)
    private String originalName;

    @Column(name = "stored_name",nullable = false,length = 255)
    private String storedName;

    @Column(name="description",nullable=false,length=255)
    private String description;


    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name="file_size",nullable = false)
    private Long fileSize;

    @CreationTimestamp
    @Column(name="uploaded_at",nullable=false)
    private LocalDateTime uploadedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status",  nullable = false)
    private FileStatus status;
}
