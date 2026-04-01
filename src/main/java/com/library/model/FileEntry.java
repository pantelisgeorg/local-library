package com.library.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_entries")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private String fileUrl;

    private Long fileSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Folder folder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public String getFormattedFileSize() {
        if (fileSize == null) return "Unknown";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.2f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
    }

    public String getFileExtension() {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf(".") + 1).toUpperCase();
    }

    public String getFileIconClass() {
        String ext = getFileExtension().toLowerCase();
        return switch (ext) {
            case "pdf" -> "fa-file-pdf";
            case "epub" -> "fa-book";
            case "txt" -> "fa-file-lines";
            case "html", "htm" -> "fa-file-code";
            case "doc", "docx" -> "fa-file-word";
            case "xls", "xlsx" -> "fa-file-excel";
            case "zip", "rar", "7z" -> "fa-file-zipper";
            case "jpg", "jpeg", "png", "gif", "svg" -> "fa-file-image";
            case "mp3", "wav", "flac" -> "fa-file-audio";
            case "mp4", "avi", "mkv" -> "fa-file-video";
            default -> "fa-file";
        };
    }

    public String getFormattedDate() {
        if (createdAt == null) return "N/A";
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy HH:mm");
        return createdAt.format(formatter);
    }
}
