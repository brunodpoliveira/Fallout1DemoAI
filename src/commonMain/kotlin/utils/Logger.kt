package utils

import java.util.Properties
import java.io.FileInputStream
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date

enum class LogLevel {
    DEBUG, WARN, ERROR
}

object Logger {
    private val _environment: String by lazy {
        System.getenv("KORGE_ENV") ?: System.getProperty("korge.env") ?: loadFromConfigFile() ?: "dev"
    }

    val environment: String
        get() = _environment

    private var currentLogLevel = when (environment) {
        "prod", "production" -> LogLevel.ERROR
        else -> LogLevel.DEBUG
    }

    private var isFileLoggingEnabled = false
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

    init {
        val config = loadConfig()
        isFileLoggingEnabled = config.getProperty("logging.file.enabled", "false").toBoolean()
        if (isFileLoggingEnabled) {
            val logFileName = config.getProperty("logging.file.name", "application.log")
            logFile = File(logFileName)
        }
    }

    private fun loadConfig(): Properties {
        val props = Properties()
        try {
            Logger::class.java.getResourceAsStream("/config.properties")?.use {
                props.load(it)
            } ?: run {
                val currentDir = System.getProperty("user.dir")
                val configFile = File(currentDir, "config.properties")
                if (configFile.exists()) {
                    FileInputStream(configFile).use { props.load(it) }
                }
            }
        } catch (e: Exception) {
            println("Error loading config.properties: ${e.message}")
        }
        return props
    }

    private fun loadFromConfigFile(): String? = loadConfig().getProperty("korge.env")

    fun setLogLevel(level: LogLevel) {
        currentLogLevel = level
    }

    fun enableFileLogging(fileName: String = "application.log") {
        isFileLoggingEnabled = true
        logFile = File(fileName)
    }

    fun disableFileLogging() {
        isFileLoggingEnabled = false
        logFile = null
    }

    private fun log(level: LogLevel, message: String) {
        if (level >= currentLogLevel) {
            val formattedMessage = "[${dateFormat.format(Date())}] [${level.name}] $message"
            println(formattedMessage)
            if (isFileLoggingEnabled && logFile != null) {
                try {
                    logFile?.let { FileWriter(it, true).use { it.write("$formattedMessage\n") } }
                } catch (e: Exception) {
                    println("Error writing to log file: ${e.message}")
                }
            }
        }
    }

    fun debug(message: String) = log(LogLevel.DEBUG, message)
    fun warn(message: String) = log(LogLevel.WARN, message)
    fun error(message: String) = log(LogLevel.ERROR, message)

    fun logWithContext(level: LogLevel, context: String, message: String) {
        log(level, "[$context] $message")
    }
}
