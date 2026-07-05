package com.lumodroid.tools

import android.content.Context
import com.lumodroid.agent.Tool
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.util.concurrent.TimeUnit

class WebSearchTool : Tool("web_search") {
    override val description = "Search the web for current information. Returns titles, snippets, and URLs from DuckDuckGo."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "query" to mapOf("type" to "string", "description" to "The search query")
        ),
        "required" to listOf("query"),
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val query = args["query"] as? String ?: return "Error: missing 'query' parameter"
        return try {
            val url = "https://html.duckduckgo.com/html/?q=${java.net.URLEncoder.encode(query, "UTF-8")}"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
                .build()
            val response = client.newCall(request).execute()
            val html = response.body?.string() ?: return "No results"
            val doc = Jsoup.parse(html)
            val results = doc.select(".result")
            if (results.isEmpty()) return "No results found for: $query"
            val sb = StringBuilder()
            for ((i, result) in results.take(10).withIndex()) {
                val title = result.selectFirst(".result__title")?.text() ?: continue
                val snippet = result.selectFirst(".result__snippet")?.text() ?: ""
                val link = result.selectFirst(".result__a")?.attr("href") ?: ""
                val cleanUrl = cleanDuckDuckGoLink(link)
                sb.appendLine("${i + 1}. $title")
                sb.appendLine("   URL: $cleanUrl")
                if (snippet.isNotBlank()) sb.appendLine("   $snippet")
                sb.appendLine()
            }
            sb.toString().trim().ifEmpty { "No results found for: $query" }
        } catch (e: Exception) {
            "Search error: ${e.message}"
        }
    }

    private fun cleanDuckDuckGoLink(link: String): String {
        if (link.startsWith("//")) return "https:$link"
        if (link.contains("uddg=")) {
            val idx = link.indexOf("uddg=")
            val encoded = link.substring(idx + 5).split("&").firstOrNull() ?: return link
            return java.net.URLDecoder.decode(encoded, "UTF-8")
        }
        return link
    }
}
