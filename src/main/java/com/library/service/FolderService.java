package com.library.service;

import com.library.model.FileEntry;
import com.library.model.Folder;
import com.library.model.FolderDto;
import com.library.repository.FileEntryRepository;
import com.library.repository.FolderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FolderService {

    private final FolderRepository folderRepository;
    private final FileEntryRepository fileEntryRepository;

    public List<Folder> getAllRootFolders() {
        return folderRepository.findByParentIsNull();
    }

    public List<Folder> getSubfolders(Long parentId) {
        return folderRepository.findByParentId(parentId);
    }

    public Optional<Folder> getFolderById(Long id) {
        return folderRepository.findById(id);
    }

    /**
     * Get folder with its subfolders and files loaded.
     * Uses separate queries to avoid Hibernate MultipleBagFetchException.
     */
    public Folder getFolderWithContents(Long id) {
        Folder folder = folderRepository.findByIdWithSubfolders(id)
                .orElse(null);

        if (folder != null) {
            // Get files separately
            List<FileEntry> files = fileEntryRepository.findByFolderIdOrderByTitleAsc(id);
            folder.getFiles().addAll(files);
        }

        return folder;
    }

    @Transactional
    public Folder createFolder(FolderDto dto) {
        Folder folder = Folder.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();

        if (dto.getParentId() != null) {
            folderRepository.findById(dto.getParentId()).ifPresent(folder::setParent);
        }

        return folderRepository.save(folder);
    }

    @Transactional
    public Folder updateFolder(Long id, FolderDto dto) {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + id));

        folder.setName(dto.getName());
        folder.setDescription(dto.getDescription());

        if (dto.getParentId() != null && !dto.getParentId().equals(id)) {
            folderRepository.findById(dto.getParentId()).ifPresent(parent -> {
                if (!isDescendant(parent, id)) {
                    folder.setParent(parent);
                }
            });
        } else if (dto.getParentId() == null) {
            folder.setParent(null);
        }

        return folderRepository.save(folder);
    }

    private boolean isDescendant(Folder potentialDescendant, Long ancestorId) {
        if (potentialDescendant.getId().equals(ancestorId)) {
            return true;
        }
        if (potentialDescendant.getParent() != null) {
            return isDescendant(potentialDescendant.getParent(), ancestorId);
        }
        return false;
    }

    @Transactional
    public void deleteFolder(Long id) throws IOException {
        Folder folder = folderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Folder not found with id: " + id));

        // Delete all files in this folder (including physical files)
        List<FileEntry> files = fileEntryRepository.findByFolderId(id);
        for (FileEntry file : files) {
            Path filePath = Paths.get(file.getFileUrl());
            if (Files.exists(filePath)) {
                Files.delete(filePath);
            }
            fileEntryRepository.deleteById(file.getId());
        }

        // Recursively delete subfolders
        deleteSubfoldersRecursively(id);

        // Delete the folder
        folderRepository.deleteById(id);
    }

    private void deleteSubfoldersRecursively(Long parentId) throws IOException {
        List<Folder> subfolders = folderRepository.findByParentId(parentId);
        for (Folder subfolder : subfolders) {
            // Delete files in subfolder
            List<FileEntry> files = fileEntryRepository.findByFolderId(subfolder.getId());
            for (FileEntry file : files) {
                Path filePath = Paths.get(file.getFileUrl());
                if (Files.exists(filePath)) {
                    Files.delete(filePath);
                }
                fileEntryRepository.deleteById(file.getId());
            }
            // Recurse into sub-subfolders
            deleteSubfoldersRecursively(subfolder.getId());
            // Delete subfolder
            folderRepository.deleteById(subfolder.getId());
        }
    }

    public boolean existsByNameInParent(String name, Long parentId) {
        if (parentId == null) {
            return folderRepository.existsByNameAndParentIsNull(name);
        }
        return folderRepository.existsByNameAndParentId(name, parentId);
    }

    public List<Folder> getBreadcrumb(Long folderId) {
        return getBreadcrumbRecursive(folderId, new ArrayList<>());
    }

    private List<Folder> getBreadcrumbRecursive(Long folderId, List<Folder> breadcrumb) {
        Optional<Folder> folder = folderRepository.findById(folderId);
        if (folder.isPresent()) {
            Folder f = folder.get();
            if (f.getParent() != null) {
                getBreadcrumbRecursive(f.getParent().getId(), breadcrumb);
            }
            breadcrumb.add(f);
        }
        return breadcrumb;
    }

    public List<Folder> getAllFolders() {
        return folderRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public List<Folder> getFolderTree() {
        List<Folder> roots = folderRepository.findByParentIsNull();
        for (Folder root : roots) {
            loadSubtree(root);
        }
        return roots;
    }

    private void loadSubtree(Folder folder) {
        for (Folder sub : folder.getSubfolders()) {
            loadSubtree(sub);
        }
    }
}
