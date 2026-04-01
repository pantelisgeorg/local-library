# Local Library - Personal File Management System

A simple, single-user library management application built with Spring Boot and Thymeleaf for organizing files and ebooks from external disks.

## Features

- **Folder Management**: Create folders and subfolders to organize your files by subject
- **File Upload**: Upload any type of file (PDF, EPUB, TXT, HTML, DOC, etc.) to folders
- **HTML Bundle Support**: Upload HTML files with companion asset folders (images, CSS, JS) as a ZIP — they are automatically extracted and rendered with all assets intact
- **Metadata**: Add title and description to each file
- **Search**: Search files by title or description
- **File Type Icons**: Visual indicators for different file types
- **Breadcrumb Navigation**: Easy navigation through folder hierarchy
- **Download**: Download files directly from the library
- **Edit/Delete**: Update file metadata or remove files and folders

## Prerequisites

- Java 17 or higher
- Maven 3.8+

## Setup Instructions

### 1. Database

The application uses an embedded **H2 database** — no external database server needed. The database file (`library-db.mv.db`) is created automatically in the project root on first run. An H2 web console is available at `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:file:./library-db`, user: `sa`, no password).

### 2. Build the Application

```bash
mvn clean package -DskipTests
```

### 4. Run the Application

```bash
java -jar target/local-library-1.0.0.jar
or mvn spring-boot:run  in the app's root directory
```

The application will start on `http://localhost:8080`

## Usage Guide

### Creating a Folder

1. Navigate to the location where you want to create a folder
2. Click the "New Folder" button
3. Enter a folder name and optional description
4. Click "Create Folder"

### Uploading a File

1. Navigate to the folder where you want to upload the file
2. Click the "Upload File" button
3. Enter a title for the file
4. Optionally add a description
5. Select the file from your external disk
6. Click "Upload"

### Uploading HTML Files with Images/Assets

When you save a webpage, you typically get a `.html` file and a companion folder (e.g., `page_files/`) containing images, CSS, and JS. To upload these with all assets intact:

1. Zip the `.html` file and its companion folder together into a single `.zip` file
2. Upload the `.zip` through the normal upload form
3. The app automatically detects the HTML inside, extracts the bundle, and renders it with all images and styling

### Searching Files

Use the search bar in the navigation menu to search files by title or description.

### File Operations

- **View**: Click on a file to see its details
- **Download**: Click the download button to save the file locally
- **Edit**: Modify the title and description
- **Delete**: Remove the file from the library

### Managing Folders

- Click on a folder to enter it
- Use the Edit button to rename or move folders
- Use the Delete button to remove folders (with all contents)

## Project Structure

```
local-library/
├── pom.xml                          # Maven configuration
├── src/main/
│   ├── java/com/library/
│   │   ├── LocalLibraryApplication.java    # Main application class
│   │   ├── controller/
│   │   │   └── LibraryController.java      # Web controller
│   │   ├── model/
│   │   │   ├── Folder.java                  # Folder entity
│   │   │   ├── FileEntry.java                # File entry entity
│   │   │   ├── FolderDto.java                # Folder data transfer object
│   │   │   └── FileEntryDto.java             # File entry DTO
│   │   ├── repository/
│   │   │   ├── FolderRepository.java         # Folder JPA repository
│   │   │   └── FileEntryRepository.java      # File entry JPA repository
│   │   └── service/
│   │       ├── FolderService.java            # Folder business logic
│   │       └── FileEntryService.java         # File entry business logic
│   └── resources/
│       ├── application.properties            # Application configuration
│       ├── static/
│       │   ├── css/style.css                 # Custom styles
│       │   └── js/app.js                     # Client-side JavaScript
│       └── templates/
│           ├── layout.html                   # Base layout template
│           ├── index.html                    # Main page
│           ├── folder-edit.html             # Folder edit page
│           ├── file-view.html                # File details page
│           ├── file-edit.html                # File edit page
│           └── search.html                   # Search results page
└── uploads/                                  # Directory for uploaded files
```

## Supported File Types

| Extension | Type | Icon |
|-----------|------|------|
| PDF | PDF Document | Red |
| EPUB | E-Book | Green |
| TXT | Text File | Gray |
| HTML/HTM | Web Page (upload as ZIP with assets) | Orange |
| DOC/DOCX | Word Document | Blue |
| XLS/XLSX | Excel Spreadsheet | Green |
| ZIP/RAR/7Z | Archive | Purple |
| Images | Images | Pink |
| Audio | Audio Files | Cyan |
| Video | Video Files | Purple |

## Configuration Options

### Upload Directory

Default: `./uploads` (relative to application directory)

To change, edit `application.properties`:

```properties
app.upload.dir=/path/to/your/upload/directory
```

### Maximum File Size

Default: 500MB

To change, edit `application.properties`:

```properties
spring.servlet.multipart.max-file-size=1GB
spring.servlet.multipart.max-request-size=1GB
```
<img width="1914" height="943" alt="library" src="https://github.com/user-attachments/assets/c0527291-d2fe-4e3b-8346-5a4f754c2fd6" />

## License

This project is for personal use.
