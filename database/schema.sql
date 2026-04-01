-- =====================================================
-- Local Library Database Schema
-- =====================================================
-- Run this script to create the database and tables
-- =====================================================

-- Create database (if not exists)
CREATE DATABASE IF NOT EXISTS local_library;
USE local_library;

-- =====================================================
-- Table: folders
-- Stores folder hierarchy for organizing files
-- =====================================================
CREATE TABLE IF NOT EXISTS folders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    parent_id BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Self-referential foreign key for subfolders
    CONSTRAINT fk_folder_parent
        FOREIGN KEY (parent_id)
        REFERENCES folders(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for faster folder lookups by parent
CREATE INDEX idx_folders_parent_id ON folders(parent_id);

-- Index for searching folders by name
CREATE INDEX idx_folders_name ON folders(name);


-- =====================================================
-- Table: file_entries
-- Stores metadata about uploaded files
-- =====================================================
CREATE TABLE IF NOT EXISTS file_entries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(255) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    file_size BIGINT,
    folder_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Foreign key to parent folder
    CONSTRAINT fk_file_folder
        FOREIGN KEY (folder_id)
        REFERENCES folders(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index for faster file lookups by folder
CREATE INDEX idx_file_entries_folder_id ON file_entries(folder_id);

-- Index for searching files by title
CREATE INDEX idx_file_entries_title ON file_entries(title);

-- Index for filtering files by type
CREATE INDEX idx_file_entries_file_type ON file_entries(file_type);
