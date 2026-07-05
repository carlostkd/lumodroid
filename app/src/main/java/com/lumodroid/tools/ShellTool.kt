package com.lumodroid.tools

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import com.lumodroid.agent.Tool
import java.io.File
import kotlin.concurrent.thread

class ShellTool : Tool("run_shell") {
    override val description = "Execute a shell command on the device. Commands not available in the app sandbox (like dig, nslookup, whois, nmap, traceroute, curl, tcpdump, etc.) are automatically routed to Termux if installed. Output is captured and returned."
    override val parameters = mapOf(
        "type" to "object",
        "properties" to mapOf(
            "command" to mapOf("type" to "string", "description" to "Shell command to execute")
        ),
        "required" to listOf("command"),
    )

    companion object {
        private val TERMUX_COMMANDS = setOf(
            "dig", "nslookup", "whois", "nmap", "curl", "wget", "tcpdump",
            "traceroute", "nc", "netcat", "ssh", "scp", "rsync", "tar",
            "ffmpeg", "convert", "python", "pip", "node", "npm", "ruby",
            "perl", "php", "git", "htop", "iftop", "nethogs", "ss", "ip",
            "ifconfig", "arp", "route", "iptables", "lsof", "strace"
        )
    }

    private fun isBinaryAvailable(binary: String): Boolean {
        return try {
            val paths = System.getenv("PATH")?.split(":")
                ?: listOf("/system/bin", "/system/xbin", "/vendor/bin", "/sbin")
            paths.any { File("$it/$binary").exists() }
        } catch (e: Exception) {
            false
        }
    }

    private fun needsTermux(command: String): Boolean {
        val binary = command.trim().split("\\s+".toRegex()).firstOrNull()?.removeSuffix("/") ?: return false
        if (binary in TERMUX_COMMANDS && !isBinaryAvailable(binary)) return true
        return false
    }

    private fun isTermuxInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo("com.termux", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    override suspend fun execute(args: Map<String, Any?>, context: Context): String {
        val command = args["command"] as? String ?: return "Error: missing 'command'"
        return try {
            // Intercept pm list packages and handle via PackageManager API
            if (command.trim().startsWith("pm list packages")) {
                handlePmListPackages(command, context)
            } else if (needsTermux(command) && isTermuxInstalled(context)) {
                runViaTermux(command, context)
            } else {
                runNative(command)
            }
        } catch (e: SecurityException) {
            "Permission denied: cannot execute shell commands"
        } catch (e: Exception) {
            "Shell error: ${e.message}"
        }
    }

    private fun handlePmListPackages(command: String, context: Context): String {
        val includeSystem = command.contains("-s") || command.contains("system")
        val pm = context.packageManager
        val apps = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.getInstalledApplications(android.content.pm.PackageManager.ApplicationInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.getInstalledApplications(0)
        }
        val sb = StringBuilder()
        for (app in apps) {
            if (!includeSystem && (app.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0) continue
            val label = pm.getApplicationLabel(app)
            sb.appendLine("package:${app.packageName}  ($label)")
        }
        return sb.toString().trimEnd().take(8000)
    }

    private fun runNative(command: String): String {
        val runtime = Runtime.getRuntime()
        val process = runtime.exec(arrayOf("sh", "-c", command))
        val stdout = process.inputStream.bufferedReader().readText()
        val stderr = process.errorStream.bufferedReader().readText()
        val exitCode = process.waitFor()
        val sb = StringBuilder()
        if (stdout.isNotBlank()) sb.appendLine(stdout.take(8000))
        if (stderr.isNotBlank()) sb.appendLine("STDERR: ${stderr.take(2000)}")
        sb.append("Exit code: $exitCode")
        return sb.toString().trimEnd()
    }

    private fun runViaTermux(command: String, context: Context): String {
        val resultDir = File(Environment.getExternalStorageDirectory(), "LumoDroid/termux_results").apply { mkdirs() }
        val resultFile = File(resultDir, "result_${System.currentTimeMillis()}.txt")

        // Clean old results
        resultDir.listFiles()?.forEach { if (it.lastModified() < System.currentTimeMillis() - 60000) it.delete() }

        return try {
            // Wrap the command so it writes its own output to a file we can read
            val wrappedCommand = "$command > ${resultFile.absolutePath} 2>&1"

            val intent = Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/sh")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", wrappedCommand))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                // Run in background (APP_SHELL runner)
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", true)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }

            // Wait for result file to be populated (up to 20 seconds)
            var waited = 0
            while (waited < 20000) {
                if (resultFile.exists() && resultFile.length() > 0) {
                    Thread.sleep(300)
                    val output = resultFile.readText().take(8000)
                    resultFile.delete()
                    return if (output.isNotBlank()) {
                        // Detect "command not found" and suggest installation
                        val binary = command.trim().split("\\s+".toRegex()).firstOrNull() ?: ""
                        if (output.contains("not found") || output.contains("not installed") || output.contains("command not found")) {
                            "$output\n\n💡 Tip: Install '$binary' in Termux by running: `pkg install $binary`\nWould you like me to install it? (requires run_shell with: pkg install $binary)"
                        } else {
                            output
                        }
                    } else "Command completed (no output)"
                }
                // Also check if file exists but is empty — command may have finished with no output
                if (resultFile.exists() && waited > 5000) {
                    Thread.sleep(500)
                    if (resultFile.length() == 0L) {
                        resultFile.delete()
                        return "Command completed (no output)"
                    }
                }
                Thread.sleep(200)
                waited += 200
            }

            // Timeout — fall back to visual session
            val visualIntent = Intent().apply {
                setClassName("com.termux", "com.termux.app.RunCommandService")
                action = "com.termux.RUN_COMMAND"
                putExtra("com.termux.RUN_COMMAND_PATH", "/data/data/com.termux/files/usr/bin/sh")
                putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-c", command))
                putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
                putExtra("com.termux.RUN_COMMAND_BACKGROUND", false)
                putExtra("com.termux.RUN_COMMAND_SESSION_ACTION", "0")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(visualIntent)
            } else {
                context.startService(visualIntent)
            }
            "Command timed out waiting for output. Opened Termux visual session for: `$command`"
        } catch (e: Exception) {
            "Failed to send command to Termux: ${e.message}\nFalling back to native shell...\n${runNative(command)}"
        }
    }
}
