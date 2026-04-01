package com.library.repository;

import com.library.model.Folder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    List<Folder> findByParentIsNull();

    List<Folder> findByParentId(Long parentId);

    Optional<Folder> findByNameAndParentId(String name, Long parentId);

    Optional<Folder> findByNameAndParentIsNull(String name);

    @Query("SELECT DISTINCT f FROM Folder f LEFT JOIN FETCH f.subfolders WHERE f.id = :id")
    Optional<Folder> findByIdWithSubfolders(@Param("id") Long id);

    @Query("SELECT DISTINCT f FROM Folder f LEFT JOIN FETCH f.files WHERE f.id = :id")
    Optional<Folder> findByIdWithFiles(@Param("id") Long id);

    boolean existsByNameAndParentId(String name, Long parentId);

    boolean existsByNameAndParentIsNull(String name);

    List<Folder> findAllByOrderByNameAsc();
}
