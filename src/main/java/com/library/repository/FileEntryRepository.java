package com.library.repository;

import com.library.model.FileEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FileEntryRepository extends JpaRepository<FileEntry, Long> {

    List<FileEntry> findByFolderId(Long folderId);

    List<FileEntry> findByFolderIdOrderByTitleAsc(Long folderId);

    Optional<FileEntry> findByFileName(String fileName);

    @Query("SELECT f FROM FileEntry f WHERE LOWER(f.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<FileEntry> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT f FROM FileEntry f WHERE f.fileType = :fileType")
    List<FileEntry> findByFileType(@Param("fileType") String fileType);

    @Query("SELECT f FROM FileEntry f LEFT JOIN FETCH f.folder WHERE f.id = :id")
    Optional<FileEntry> findByIdWithFolder(@Param("id") Long id);

    boolean existsByFileNameAndFolderId(String fileName, Long folderId);

    long countByFolderId(Long folderId);
}
