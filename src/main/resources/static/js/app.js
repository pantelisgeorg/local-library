// Local Library - JavaScript Functions

document.addEventListener('DOMContentLoaded', function() {
    // Auto-hide alerts after 5 seconds
    const alerts = document.querySelectorAll('.alert');
    alerts.forEach(function(alert) {
        setTimeout(function() {
            const bsAlert = new bootstrap.Alert(alert);
            bsAlert.close();
        }, 5000);
    });

    // Form validation
    const forms = document.querySelectorAll('form');
    forms.forEach(function(form) {
        form.addEventListener('submit', function(event) {
            if (!form.checkValidity()) {
                event.preventDefault();
                event.stopPropagation();
            }
            form.classList.add('was-validated');
        });
    });

    // File input - update label with selected filename
    const fileInput = document.getElementById('file');
    if (fileInput) {
        fileInput.addEventListener('change', function() {
            const fileName = this.files[0]?.name || 'Choose file...';
            // Find the closest label or create a visual indicator
            const formText = this.closest('.mb-3')?.querySelector('.form-text');
            if (formText) {
                formText.textContent = 'Selected: ' + fileName;
            }
        });
    }

    // Confirm delete actions for forms without inline onsubmit
    const deleteForms = document.querySelectorAll('form[method="post"]');
    deleteForms.forEach(function(form) {
        const action = form.getAttribute('action') || '';
        if (action.includes('delete')) {
            form.addEventListener('submit', function(event) {
                if (!confirm('Are you sure you want to delete this item? This action cannot be undone.')) {
                    event.preventDefault();
                }
            });
        }
    });

    // Build folder tree sidebar
    buildFolderTree();
    
    // Add fade-in animation to main content
    const mainCard = document.querySelector('.main-card');
    if (mainCard) {
        mainCard.classList.add('fade-in');
    }
});

// Build cascading folder tree from flat data nodes
function buildFolderTree() {
    var container = document.getElementById('folderTree');
    if (!container) return;

    var currentFolderId = container.getAttribute('data-current-folder');
    var nodes = container.querySelectorAll('.folder-tree-node');
    if (nodes.length === 0) return;

    // Collect folder data
    var folders = [];
    nodes.forEach(function(node) {
        folders.push({
            id: node.getAttribute('data-id'),
            parentId: node.getAttribute('data-parent-id') || '',
            link: node.querySelector('a').cloneNode(true),
            name: node.querySelector('span').textContent
        });
    });

    // Remove original hidden nodes
    nodes.forEach(function(node) { node.remove(); });

    // Build children map
    var childrenMap = {};
    folders.forEach(function(f) {
        var pid = f.parentId || 'root';
        if (!childrenMap[pid]) childrenMap[pid] = [];
        childrenMap[pid].push(f);
    });

    // Find ancestors of current folder for auto-expand
    var expandedIds = {};
    if (currentFolderId) {
        var folderMap = {};
        folders.forEach(function(f) { folderMap[f.id] = f; });
        var cur = folderMap[currentFolderId];
        while (cur && cur.parentId) {
            expandedIds[cur.parentId] = true;
            cur = folderMap[cur.parentId];
        }
    }

    // Recursively build tree DOM
    function buildLevel(parentId, depth) {
        var children = childrenMap[parentId || 'root'];
        if (!children || children.length === 0) return null;

        var wrapper = document.createElement('div');
        wrapper.className = 'folder-tree-level';
        if (depth > 0) {
            wrapper.classList.add('folder-children');
            if (!expandedIds[parentId]) {
                wrapper.style.display = 'none';
            }
        }

        children.forEach(function(f) {
            var item = document.createElement('div');
            item.className = 'folder-tree-item';

            var row = document.createElement('div');
            row.className = 'folder-tree-row';
            row.style.paddingLeft = (depth * 1.25 + 0.5) + 'rem';

            if (currentFolderId && f.id === currentFolderId) {
                row.classList.add('active');
            }

            var hasChildren = childrenMap[f.id] && childrenMap[f.id].length > 0;

            // Toggle chevron
            var toggle = document.createElement('span');
            if (hasChildren) {
                toggle.className = 'folder-toggle' + (expandedIds[f.id] ? ' open' : '');
                toggle.innerHTML = '<i class="fas fa-chevron-right fa-xs"></i>';
                toggle.addEventListener('click', function(e) {
                    e.preventDefault();
                    e.stopPropagation();
                    var childrenDiv = item.querySelector(':scope > .folder-children');
                    if (childrenDiv) {
                        var isOpen = childrenDiv.style.display !== 'none';
                        childrenDiv.style.display = isOpen ? 'none' : 'block';
                        toggle.classList.toggle('open', !isOpen);
                    }
                });
            } else {
                toggle.className = 'folder-toggle-placeholder';
            }
            row.appendChild(toggle);

            // Folder link
            var link = f.link;
            link.classList.add('folder-tree-link');
            row.appendChild(link);

            item.appendChild(row);

            // Recursively build children
            if (hasChildren) {
                var childLevel = buildLevel(f.id, depth + 1);
                if (childLevel) {
                    item.appendChild(childLevel);
                }
            }

            wrapper.appendChild(item);
        });

        return wrapper;
    }

    var tree = buildLevel('root', 0);
    if (tree) {
        container.appendChild(tree);
    }
}

// Format file size helper
function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

// Utility: Debounce function for search inputs
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}
