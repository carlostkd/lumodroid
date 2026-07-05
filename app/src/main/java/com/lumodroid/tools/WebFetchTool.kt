package com.lumodroid.tools

import android.content.Context
import com.lumodroid.agent.Tool
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.safety.Safelist
import java.util.concurrent.TimeUnit

class WebFetchTool : Tool("web_extract") {
    override val description = "Fetch a web page URL and extract its main content as readable text. Useful for reading articles, documentation, or any web page content."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "url" to mapOf("type" to "string", "description" to "The full URL to fetch (including https://)")
        ),
        "required" to listOf("url"),
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val rawUrl = args["url"] as? String ?: return "Error: missing 'url' parameter"
        val url = if (!rawUrl.startsWith("http")) "https://$rawUrl" else rawUrl
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return "Empty response from server"
            if (html.isBlank()) return "Empty page content"
            val doc = Jsoup.parse(html)
            doc.select("script, style, nav, footer, header, .sidebar, .ads, .ad, .advertisement, .cookie-banner, noscript, iframe, form").remove()
            val title = doc.title().ifBlank { "(no title)" }
            val metaDesc = doc.selectFirst("meta[name=description]")?.attr("content") ?: ""
            val body = doc.body() ?: return "Title: $title\n\n(no body content)"
            val articleTag = body.selectFirst("article") ?: body.selectFirst("main") ?: body.selectFirst("[role=main]")
            val contentEl = articleTag ?: body
            val cleanedHtml = Jsoup.clean(contentEl.html(), Safelist.none())
            val text = cleanedHtml
                .replace("(?m)^\\s+$".toRegex(), "")
                .replace("\\n{3,}".toRegex(), "\n\n")
                .trim()
            val truncated = if (text.length > 10000) "${text.take(10000)}\n\n[... truncated ...]" else text
            val sb = StringBuilder()
            sb.appendLine("Title: $title")
            if (metaDesc.isNotBlank()) sb.appendLine("Description: $metaDesc")
            sb.appendLine("URL: $url")
            sb.appendLine()
            sb.append(truncated.ifBlank { "(no extractable text content)" })
            sb.toString()
        } catch (e: Exception) {
            "Fetch error: ${e.message}"
        }
    }
}
