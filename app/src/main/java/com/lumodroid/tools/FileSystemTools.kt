package com.lumodroid.tools

import android.content.Context
import android.os.Environment
import com.lumodroid.agent.Tool
import java.io.File

class ListFilesTool : Tool("list_files") {
    override val description = "List files and directories at a given path. Supports recursive listing, sorting by size, folder size calculation, and filtering by minimum size."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "path" to mapOf("type" to "string", "description" to "Directory path (default: /storage/emulated/0)"),
            "recursive" to mapOf("type" to "boolean", "description" to "If true, list files recursively in subdirectories (default: false)"),
            "min_size_mb" to mapOf("type" to "number", "description" to "Only show files/folders larger than this size in MB (default: 0 = show all)"),
            "sort_by_size" to mapOf("type" to "boolean", "description" to "If true, sort results by size descending (largest first) (default: false)"),
            "max_depth" to mapOf("type" to "integer", "description" to "Maximum recursion depth when recursive=true (default: 5)"),
            "files_only" to mapOf("type" to "boolean", "description" to "If true, only show files (not directories) (default: false)"),
            "show_folder_sizes" to mapOf("type" to "boolean", "description" to "If true, calculate total size of each directory (default: false)")
        ),
        "required" to emptyList<String>(),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val path = args["path"] as? String ?: Environment.getExternalStorageDirectory().absolutePath
        val recursive = args["recursive"] as? Boolean ?: false
        val minSizeMb = (args["min_size_mb"] as? Number)?.toDouble() ?: 0.0
        val sortBySize = args["sort_by_size"] as? Boolean ?: false
        val maxDepth = (args["max_depth"] as? Number)?.toInt() ?: 5
        val filesOnly = args["files_only"] as? Boolean ?: false
        val showFolderSizes = args["show_folder_sizes"] as? Boolean ?: false

        return try {
            val dir = File(path)
            if (!dir.exists()) return "Path does not exist: $path"
            if (!dir.isDirectory) return "Not a directory: $path"

            val minSizeBytes = (minSizeMb * 1024 * 1024).toLong()
            val results = mutableListOf<FileEntry>()

            if (recursive) {
                dir.walkTopDown().maxDepth(maxDepth).forEach { f ->
                    if (f == dir) return@forEach
                    if (filesOnly && f.isDirectory) return@forEach
                    val size = if (f.isDirectory) calcDirSize(f) else f.length()
                    if (minSizeMb > 0 && size < minSizeBytes) return@forEach
                    results.add(FileEntry(f.absolutePath, f.name, size, f.isDirectory))
                }
            } else {
                dir.listFiles()?.forEach { f ->
                    if (filesOnly && f.isDirectory) return@forEach
                    val size = if (f.isDirectory) calcDirSize(f) else f.length()
                    if (minSizeMb > 0 && size < minSizeBytes) return@forEach
                    results.add(FileEntry(f.absolutePath, f.name, size, f.isDirectory))
                }
            }

            if (results.isEmpty()) {
                val filterDesc = if (minSizeMb > 0) " larger than ${minSizeMb}MB" else ""
                return "No files$filterDesc found in $path"
            }

            val sorted = if (sortBySize) {
                results.sortedByDescending { it.size }
            } else {
                results.sortedBy { it.name.lowercase() }
            }

            sorted.joinToString("\n") { entry ->
                val icon = if (entry.isDirectory) "📁" else "📄"
                val size = if (entry.isDirectory) {
                    if (showFolderSizes) " (${formatSize(entry.size)})" else ""
                } else {
                    " (${formatSize(entry.size)})"
                }
                "$icon ${entry.name}$size"
            }
        } catch (e: Exception) {
            "Error listing files: ${e.message}"
        }
    }

    private data class FileEntry(val path: String, val name: String, val size: Long, val isDirectory: Boolean)

    private fun calcDirSize(dir: File): Long {
        var size = 0L
        try {
            dir.walkTopDown().forEach { f ->
                if (f.isFile) size += f.length()
            }
        } catch (_: Exception) {}
        return size
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
            else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
        }
    }
}

class SearchFilesTool : Tool("search_files") {
    override val description = "Search for files by name pattern across device storage. Returns matching files with sizes and paths."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "pattern" to mapOf("type" to "string", "description" to "File name pattern (e.g. *.pdf, invoice*)"),
            "directory" to mapOf("type" to "string", "description" to "Directory to start search from (default: /storage/emulated/0)"),
            "max_depth" to mapOf("type" to "integer", "description" to "Maximum recursion depth (default: 10)")
        ),
        "required" to listOf("pattern"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val pattern = args["pattern"] as? String ?: return "Error: missing 'pattern'"
        val dirPath = args["directory"] as? String ?: Environment.getExternalStorageDirectory().absolutePath
        val maxDepth = (args["max_depth"] as? Number)?.toInt() ?: 10
        return try {
            val dir = File(dirPath)
            if (!dir.exists()) return "Directory not found: $dirPath"
            val results = mutableListOf<String>()
            val regex = patternToRegex(pattern)
            dir.walkTopDown().maxDepth(maxDepth).forEach { f ->
                if (f.isFile && regex.matches(f.name)) {
                    results.add("${f.absolutePath} (${formatSize(f.length())})")
                }
            }
            if (results.isEmpty()) "No files matching '$pattern' found in $dirPath"
            else results.joinToString("\n")
        } catch (e: Exception) {
            "Search error: ${e.message}"
        }
    }

    private fun patternToRegex(pattern: String): Regex {
        val r = pattern.replace(".", "\\.").replace("*", ".*").replace("?", ".")
        return Regex(r, RegexOption.IGNORE_CASE)
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            bytes < 1024 * 1024 * 1024 -> "${"%.1f".format(bytes.toDouble() / (1024 * 1024))} MB"
            else -> "${"%.2f".format(bytes.toDouble() / (1024 * 1024 * 1024))} GB"
        }
    }
}

class ReadFileTool : Tool("read_file") {
    override val description = "Read the contents of a text file."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "path" to mapOf("type" to "string", "description" to "Full absolute path to the file")
        ),
        "required" to listOf("path"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val path = args["path"] as? String ?: return "Error: missing 'path'"
        return try {
            val file = File(path)
            if (!file.exists()) return "File not found: $path"
            if (!file.canRead()) return "Permission denied: $path"
            val text = file.readText(Charsets.UTF_8)
            text.take(10000)
        } catch (e: Exception) {
            "Read error: ${e.message}"
        }
    }
}
