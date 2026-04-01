package com.library.service;

import com.library.model.FileEntry;
import com.library.model.FileEntryDto;
import com.library.model.Folder;
import com.library.repository.FileEntryRepository;
import com.library.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileEntryService {

    private final FileEntryRepository fileEntryRepository;
    private final FolderRepository folderRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public List<FileEntry> getFilesByFolderId(Long folderId) {
        return fileEntryRepository.findByFolderIdOrderByTitleAsc(folderId);
    }

    public Optional<FileEntry> getFileById(Long id) {
        return fileEntryRepository.findByIdWithFolder(id);
    }

    @Transactional
    public FileEntry createFileEntry(FileEntryDto dto) throws IOException {
        Folder folder = folderRepository.findById(dto.getFolderId())
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + dto.getFolderId()));

        MultipartFile file = dto.getFile();
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);

        // If it's a ZIP, check if it contains an HTML file with assets
        if ("zip".equalsIgnoreCase(fileExtension)) {
            try {
                FileEntry htmlEntry = tryExtractHtmlZip(file, folder, dto);
                if (htmlEntry != null) {
                    return htmlEntry;
                }
            } catch (Exception e) {
                log.warn("Failed to extract HTML bundle from ZIP, storing as regular ZIP", e);
            }
        }

        String storedFileName = UUID.randomUUID().toString() + "_" + sanitizeFileName(originalFilename);

        Path uploadPath = Paths.get(uploadDir, folder.getId().toString());
        Files.createDirectories(uploadPath);

        Path filePath = uploadPath.resolve(storedFileName);
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
        }

        FileEntry fileEntry = FileEntry.builder()
                .title(dto.getTitle() != null && !dto.getTitle().isEmpty() ? dto.getTitle() : originalFilename)
                .description(dto.getDescription())
                .fileName(originalFilename)
                .fileType(fileExtension.toUpperCase())
                .fileUrl(filePath.toString())
                .fileSize(file.getSize())
                .folder(folder)
                .build();

        return fileEntryRepository.save(fileEntry);
    }

    /**
     * Tries to extract a ZIP as an HTML-with-assets bundle.
     * Saves the ZIP to a temp file, scans with ZipFile (random access), then extracts if valid.
     * Returns a saved FileEntry if the ZIP contains exactly one HTML/HTM file, or null otherwise.
     */
    private FileEntry tryExtractHtmlZip(MultipartFile zipMultipart, Folder folder, FileEntryDto dto) throws IOException {
        // Save to a temp file so we can use ZipFile (random access) for reliable reading
        Path tempZip = Files.createTempFile("upload-", ".zip");
        try {
            try (InputStream is = zipMultipart.getInputStream()) {
                Files.copy(is, tempZip, StandardCopyOption.REPLACE_EXISTING);
            }

            String htmlEntryName = null;
            int htmlCount = 0;

            try (ZipFile zipFile = new ZipFile(tempZip.toFile())) {
                var entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith("__MACOSX") || name.contains("/.")) {
                        continue;
                    }
                    if (!entry.isDirectory()) {
                        String ext = getFileExtension(name);
                        if ("html".equalsIgnoreCase(ext) || "htm".equalsIgnoreCase(ext)) {
                            htmlEntryName = name;
                            htmlCount++;
                        }
                    }
                }

                if (htmlCount != 1 || htmlEntryName == null) {
                    return null; // Not an HTML bundle ZIP
                }

                // Extract everything
                String uuid = UUID.randomUUID().toString();
                Path extractDir = Paths.get(uploadDir, folder.getId().toString(), uuid + "_html").normalize();
                Files.createDirectories(extractDir);

                long totalSize = 0;
                entries = zipFile.entries();
                while (entries.hasMoreElements()) {
                    ZipEntry entry = entries.nextElement();
                    String name = entry.getName();
                    if (name.startsWith("__MACOSX") || name.contains("/.")) {
                        continue;
                    }

                    Path targetPath = extractDir.resolve(name).normalize();
                    if (!targetPath.startsWith(extractDir)) {
                        continue;
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(targetPath);
                    } else {
                        Files.createDirectories(targetPath.getParent());
                        try (InputStream is = zipFile.getInputStream(entry)) {
                            Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        }
                        totalSize += Files.size(targetPath);
                    }
                }

                Path htmlFilePath = extractDir.resolve(htmlEntryName).normalize();
                if (!Files.exists(htmlFilePath)) {
                    deleteDirectoryRecursively(extractDir);
                    return null;
                }

                String htmlFileName = Paths.get(htmlEntryName).getFileName().toString();
                String htmlExt = getFileExtension(htmlFileName);

                FileEntry fileEntry = FileEntry.builder()
                        .title(dto.getTitle() != null && !dto.getTitle().isEmpty() ? dto.getTitle() : htmlFileName)
                        .description(dto.getDescription())
                        .fileName(htmlFileName)
                        .fileType(htmlExt.toUpperCase())
                        .fileUrl(htmlFilePath.toString())
                        .fileSize(totalSize)
                        .folder(folder)
                        .build();

                return fileEntryRepository.save(fileEntry);
            }
        } finally {
            Files.deleteIfExists(tempZip);
        }
    }

    @Transactional
    public FileEntry updateFileEntry(Long id, FileEntryDto dto) {
        FileEntry fileEntry = fileEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));

        if (dto.getTitle() != null) {
            fileEntry.setTitle(dto.getTitle());
        }
        if (dto.getDescription() != null) {
            fileEntry.setDescription(dto.getDescription());
        }

        return fileEntryRepository.save(fileEntry);
    }

    @Transactional
    public void deleteFileEntry(Long id) throws IOException {
        FileEntry fileEntry = fileEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found with id: " + id));

        Path filePath = Paths.get(fileEntry.getFileUrl()).normalize();

        // If file is inside an extracted HTML bundle directory (ends with _html),
        // delete the entire bundle directory
        Path dir = filePath.getParent();
        Path bundleRoot = null;
        while (dir != null) {
            if (dir.getFileName() != null && dir.getFileName().toString().endsWith("_html")) {
                bundleRoot = dir;
                break;
            }
            dir = dir.getParent();
        }
        if (bundleRoot != null) {
            deleteDirectoryRecursively(bundleRoot);
        } else if (Files.exists(filePath)) {
            Files.delete(filePath);
        }

        fileEntryRepository.deleteById(id);
    }

    private void deleteDirectoryRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (var paths = Files.walk(dir)) {
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (IOException ignored) {}
            });
        }
    }

    public List<FileEntry> searchFiles(String keyword) {
        return fileEntryRepository.searchByKeyword(keyword);
    }

    public List<FileEntry> getFilesByType(String fileType) {
        return fileEntryRepository.findByFileType(fileType.toUpperCase());
    }

    public long getFileCountInFolder(Long folderId) {
        return fileEntryRepository.countByFolderId(folderId);
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "unknown";
        }
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1) {
            return "unknown";
        }
        return filename.substring(lastDotIndex + 1).toLowerCase();
    }

    private String sanitizeFileName(String filename) {
        if (filename == null) {
            return "unknown";
        }
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    public Path getFilePath(FileEntry fileEntry) {
        return Paths.get(fileEntry.getFileUrl());
    }

    /**
     * Resolves a relative asset path against the HTML file's parent directory.
     * Used to serve companion assets (images, CSS, JS) for extracted HTML bundles.
     * Returns null if the file is not in an HTML bundle or the path escapes the bundle.
     */
    public Path getHtmlAssetPath(FileEntry fileEntry, String relativePath) {
        Path filePath = Paths.get(fileEntry.getFileUrl()).normalize();
        Path htmlParent = filePath.getParent();
        if (htmlParent == null) {
            return null;
        }

        // Find the _html bundle root directory by walking up from the HTML file
        Path bundleRoot = htmlParent;
        while (bundleRoot != null && (bundleRoot.getFileName() == null || !bundleRoot.getFileName().toString().endsWith("_html"))) {
            bundleRoot = bundleRoot.getParent();
        }
        if (bundleRoot == null) {
            return null;
        }

        // Resolve the relative path against the HTML file's parent directory
        Path assetPath = htmlParent.resolve(relativePath).normalize();
        // Security: ensure the resolved path stays within the bundle directory
        if (!assetPath.startsWith(bundleRoot)) {
            return null;
        }
        if (!Files.exists(assetPath) || Files.isDirectory(assetPath)) {
            return null;
        }
        return assetPath;
    }
}
