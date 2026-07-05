package com.lumodroid.tools

import android.content.Context
import com.lumodroid.agent.Tool
import java.io.File

class PdfExtractorTool : Tool("extract_pdf_text") {
    override val description = "Extract text content from a PDF file."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "path" to mapOf("type" to "string", "description" to "Full path to the PDF file")
        ),
        "required" to listOf("path"),
    )

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val path = args["path"] as? String ?: return "Error: missing 'path'"
        return try {
            val file = File(path)
            if (!file.exists()) return "File not found: $path"
            val document = com.tom_roush.pdfbox.pdmodel.PDDocument.load(file)
            val stripper = com.tom_roush.pdfbox.text.PDFTextStripper()
            stripper.sortByPosition = true
            val text = stripper.getText(document)
            document.close()
            if (text.isBlank()) "PDF appears to be scanned (no extractable text)"
            else text.take(15000)
        } catch (e: Exception) {
            "PDF extraction error: ${e.message}"
        }
    }
}
