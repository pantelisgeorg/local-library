package com.library.controller;

import com.library.model.FileEntry;
import com.library.model.FileEntryDto;
import com.library.model.Folder;
import com.library.model.FolderDto;
import com.library.service.FileEntryService;
import com.library.service.FolderService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class LibraryController {

    private final FolderService folderService;
    private final FileEntryService fileEntryService;

    @GetMapping("/")
    public String home(Model model) {
        List<Folder> rootFolders = folderService.getAllRootFolders();
        model.addAttribute("folders", rootFolders);
        model.addAttribute("currentFolder", null);
        model.addAttribute("breadcrumb", List.of());
        model.addAttribute("allFolders", folderService.getAllFolders());
        return "index";
    }

    @GetMapping("/folder/{id}")
    public String viewFolder(@PathVariable Long id, Model model) {
        Folder folder = folderService.getFolderById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found"));

        Folder folderWithContents = folderService.getFolderWithContents(id);
        if (folderWithContents == null) {
            throw new RuntimeException("Folder not found");
        }

        List<Folder> breadcrumb = folderService.getBreadcrumb(id);

        model.addAttribute("currentFolder", folder);
        model.addAttribute("folders", folderWithContents.getSubfolders());
        model.addAttribute("files", folderWithContents.getFiles());
        model.addAttribute("breadcrumb", breadcrumb);
        model.addAttribute("allFolders", folderService.getAllFolders());
        return "index";
    }

    @PostMapping("/folder/create")
    public String createFolder(@ModelAttribute FolderDto folderDto, RedirectAttributes redirectAttributes) {
        try {
            if (folderService.existsByNameInParent(folderDto.getName(), folderDto.getParentId())) {
                redirectAttributes.addFlashAttribute("error", "A folder with this name already exists in this location");
            } else {
                folderService.createFolder(folderDto);
                redirectAttributes.addFlashAttribute("success", "Folder created successfully");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error creating folder: " + e.getMessage());
        }

        if (folderDto.getParentId() != null) {
            return "redirect:/folder/" + folderDto.getParentId();
        }
        return "redirect:/";
    }

    @GetMapping("/folder/{id}/edit")
    public String editFolderForm(@PathVariable Long id, Model model) {
        Folder folder = folderService.getFolderById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found"));
        List<Folder> allFolders = folderService.getAllFolders();
        allFolders.removeIf(f -> f.getId().equals(id));
        model.addAttribute("folder", folder);
        model.addAttribute("allFolders", allFolders);
        return "folder-edit";
    }

    @PostMapping("/folder/{id}/edit")
    public String updateFolder(@PathVariable Long id, @ModelAttribute FolderDto folderDto, RedirectAttributes redirectAttributes) {
        try {
            folderService.updateFolder(id, folderDto);
            redirectAttributes.addFlashAttribute("success", "Folder updated successfully");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating folder: " + e.getMessage());
        }
        return "redirect:/folder/" + id;
    }

    @PostMapping("/folder/{id}/delete")
    public String deleteFolder(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            Folder folder = folderService.getFolderById(id)
                    .orElseThrow(() -> new RuntimeException("Folder not found"));
            Long parentId = folder.getParent() != null ? folder.getParent().getId() : null;

            folderService.deleteFolder(id);
            redirectAttributes.addFlashAttribute("success", "Folder deleted successfully");

            if (parentId != null) {
                return "redirect:/folder/" + parentId;
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting folder: " + e.getMessage());
        }
        return "redirect:/";
    }

    @GetMapping("/file/{id}")
    public String viewFile(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return fileEntryService.getFileById(id)
                .map(fileEntry -> {
                    model.addAttribute("file", fileEntry);

                    // Read text content for TXT files
                    if ("TXT".equals(fileEntry.getFileType())) {
                        try {
                            Path filePath = fileEntryService.getFilePath(fileEntry);
                            String content = Files.readString(filePath);
                            // Limit to first 500KB for performance
                            if (content.length() > 512000) {
                                content = content.substring(0, 512000) + "\n\n... (content truncated for preview)";
                            }
                            model.addAttribute("fileContent", content);
                        } catch (IOException e) {
                            model.addAttribute("fileContent", "Error reading file content");
                        }
                    }

                    return "file-view";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "File not found");
                    return "redirect:/";
                });
    }

    @GetMapping("/file/{id}/edit")
    public String editFileForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        return fileEntryService.getFileById(id)
                .map(fileEntry -> {
                    model.addAttribute("file", fileEntry);
                    return "file-edit";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("error", "File not found");
                    return "redirect:/";
                });
    }

    @PostMapping("/file/{id}/edit")
    public String updateFile(@PathVariable Long id, @ModelAttribute FileEntryDto fileEntryDto, RedirectAttributes redirectAttributes) {
        try {
            FileEntry fileEntry = fileEntryService.updateFileEntry(id, fileEntryDto);
            redirectAttributes.addFlashAttribute("success", "File metadata updated successfully");
            return "redirect:/folder/" + fileEntry.getFolder().getId();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error updating file: " + e.getMessage());
            return "redirect:/file/" + id;
        }
    }

    @PostMapping("/file/create")
    public String uploadFile(@ModelAttribute FileEntryDto fileEntryDto, RedirectAttributes redirectAttributes) {
        try {
            MultipartFile file = fileEntryDto.getFile();
            if (file == null || file.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
                return "redirect:/folder/" + fileEntryDto.getFolderId();
            }

            fileEntryService.createFileEntry(fileEntryDto);
            redirectAttributes.addFlashAttribute("success", "File uploaded successfully: " + file.getOriginalFilename());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error uploading file: " + e.getMessage());
        }
        return "redirect:/folder/" + fileEntryDto.getFolderId();
    }

    @PostMapping("/file/{id}/delete")
    public String deleteFile(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            FileEntry file = fileEntryService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));
            Long folderId = file.getFolder().getId();

            fileEntryService.deleteFileEntry(id);
            redirectAttributes.addFlashAttribute("success", "File deleted successfully");

            return "redirect:/folder/" + folderId;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting file: " + e.getMessage());
            return "redirect:/";
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        try {
            FileEntry fileEntry = fileEntryService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            Path filePath = fileEntryService.getFilePath(fileEntry);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = getContentType(fileEntry.getFileType());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileEntry.getFileName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/view/{id}")
    public ResponseEntity<Resource> viewFileInline(@PathVariable Long id) {
        try {
            FileEntry fileEntry = fileEntryService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            Path filePath = fileEntryService.getFilePath(fileEntry);
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = getContentType(fileEntry.getFileType());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileEntry.getFileName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/view/{id}/**")
    public ResponseEntity<Resource> viewHtmlAsset(@PathVariable Long id, HttpServletRequest request) {
        try {
            FileEntry fileEntry = fileEntryService.getFileById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            // Extract the relative path after /view/{id}/
            String fullPath = request.getRequestURI();
            String prefix = "/view/" + id + "/";
            int prefixIndex = fullPath.indexOf(prefix);
            if (prefixIndex == -1) {
                return ResponseEntity.notFound().build();
            }
            String relativePath = URLDecoder.decode(
                    fullPath.substring(prefixIndex + prefix.length()), StandardCharsets.UTF_8);

            // Empty relative path (trailing slash) — serve the main file itself
            if (relativePath.isEmpty()) {
                return viewFileInline(id);
            }

            Path assetPath = fileEntryService.getHtmlAssetPath(fileEntry, relativePath);
            if (assetPath == null) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(assetPath.toUri());
            String contentType = URLConnection.guessContentTypeFromName(assetPath.getFileName().toString());
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/search")
    public String search(@RequestParam(required = false) String keyword, @RequestParam(required = false) String type, Model model) {
        List<FileEntry> results;
        if (keyword != null && !keyword.trim().isEmpty()) {
            results = fileEntryService.searchFiles(keyword);
        } else if (type != null && !type.trim().isEmpty()) {
            results = fileEntryService.getFilesByType(type);
        } else {
            results = List.of();
        }

        model.addAttribute("results", results);
        model.addAttribute("keyword", keyword);
        model.addAttribute("type", type);
        return "search";
    }

    private String getContentType(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "pdf" -> "application/pdf";
            case "epub" -> "application/epub+zip";
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "zip" -> "application/zip";
            case "mp4" -> "video/mp4";
            case "avi" -> "video/x-msvideo";
            case "mkv" -> "video/x-matroska";
            case "mov" -> "video/quicktime";
            case "webm" -> "video/webm";
            case "wmv" -> "video/x-ms-wmv";
            case "flv" -> "video/x-flv";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "flac" -> "audio/flac";
            case "m4a" -> "audio/mp4";
            case "aac" -> "audio/aac";
            default -> "application/octet-stream";
        };
    }
}
