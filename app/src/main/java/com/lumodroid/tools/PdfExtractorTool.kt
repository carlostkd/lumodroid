package com.lumodroid.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.util.Log
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import com.lumodroid.agent.Tool
import java.io.File

class PdfExtractorTool : Tool("extract_pdf_text") {
    override val description = "Extract text content from a PDF file. Works with any PDF that has a text layer."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "path" to mapOf("type" to "string", "description" to "Full path to the PDF file")
        ),
        "required" to listOf("path"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val path = args["path"] as? String ?: return "Error: missing 'path'"
        val file = File(path)

        if (!file.exists()) return "File not found: $path"
        if (!file.canRead()) return "Cannot read file (permission denied): $path"
        if (!file.name.endsWith(".pdf", ignoreCase = true))
            return "File does not appear to be a PDF: $path"

        val fileSizeMb = file.length() / (1024.0 * 1024.0)
        if (fileSizeMb > 50) {
            return "PDF is too large (${String.format("%.1f", fileSizeMb)} MB). Maximum supported size is 50 MB."
        }

        return try {
            extractWithIText(file)
        } catch (e: Exception) {
            Log.e(TAG, "iText extraction failed, trying Termux fallback", e)
            if (isTermuxInstalled(context)) {
                tryViaTermuxPdftotext(file, context)
            } else {
                "Failed to extract PDF text: ${e.message}"
            }
        }
    }

    private fun extractWithIText(file: File): String {
        val reader = PdfReader(file.inputStream())
        val pageCount = reader.numberOfPages
        if (pageCount == 0) return "PDF has no pages."

        val sb = StringBuilder()
        for (i in 1..pageCount) {
            try {
                val pageText = PdfTextExtractor.getTextFromPage(reader, i)
                if (pageText.isNotBlank()) {
                    sb.append("=== Page $i ===\n")
                    sb.append(pageText.trim())
                    sb.append("\n\n")
                }
            } catch (e: Exception) {
                sb.append("=== Page $i ===\n(extraction error: ${e.message})\n\n")
            }

            if (sb.length > 15000) {
                sb.append("\n... [truncated, more pages remain]")
                break
            }
        }

        reader.close()

        return if (sb.isBlank()) {
            "PDF appears to be scanned or contains no extractable text layer."
        } else {
            sb.toString().trim()
        }
    }

    private fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun tryViaTermuxPdftotext(file: File, context: Context): String {
        val resultDir = File(Environment.getExternalStorageDirectory(), "LumoDroid/termux_results").apply { mkdirs() }
        val resultFile = File(resultDir, "pdf_${System.currentTimeMillis()}.txt")
        val doneFile = File(resultDir, "done_${System.currentTimeMillis()}.flag")

        resultDir.listFiles()?.forEach {
            if (it.lastModified() < System.currentTimeMillis() - 60000) it.delete()
        }

        return try {
            val script = """
                pdftotext "${file.absolutePath}" "${resultFile.absolutePath}" 2>&1 && echo "OK" > "${doneFile.absolutePath}" || echo "FAIL" > "${doneFile.absolutePath}"
            """.trimIndent()

            val intent = Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/sh")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", script))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            var waited = 0
            while (waited < 30000) {
                if (doneFile.exists()) {
                    Thread.sleep(300)
                    val status = doneFile.readText().trim()
                    doneFile.delete()

                    if (status == "OK" && resultFile.exists() && resultFile.length() > 0) {
                        val text = resultFile.readText()
                        resultFile.delete()
                        return if (text.isNotBlank()) {
                            if (text.length > 15000) text.take(15000) + "\n\n... [truncated]"
                            else text
                        } else {
                            "PDF appears to be scanned or contains no extractable text layer."
                        }
                    }
                    resultFile.delete()
                    return "pdftotext failed. Make sure poppler is installed: pkg install poppler"
                }
                Thread.sleep(200)
                waited += 200
            }

            doneFile.delete()
            resultFile.delete()
            "Timed out waiting for pdftotext."
        } catch (e: Exception) {
            Log.e(TAG, "Termux pdftotext failed", e)
            "PDF extraction failed: ${e.message}"
        }
    }

    companion object {
        private const val TAG = "PdfExtractorTool"
    }
}
