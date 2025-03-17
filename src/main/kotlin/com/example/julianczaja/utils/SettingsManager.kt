package com.example.julianczaja.utils

import com.example.julianczaja.model.DeviceServerSettings
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock


object SettingsManager {

    private const val SETTINGS_FILE_NAME = "settings.json"

    private val json = Json { prettyPrint = true; encodeDefaults = true }
    private val lock = ReentrantLock()

    private fun getDeviceSettingsFile(deviceId: Long): File {
        val deviceDir = FileHandler.getDeviceDir(deviceId)
        if (!deviceDir.exists()) {
            deviceDir.mkdirs()
        }
        return File(deviceDir, SETTINGS_FILE_NAME)
    }

    private fun createEmptySettingsFile(deviceId: Long) = saveSettings(deviceId, DeviceServerSettings())

    fun saveSettings(deviceId: Long, settings: DeviceServerSettings) {
        val settingsFile = getDeviceSettingsFile(deviceId)
        lock.withLock {
            try {
                settingsFile.writeText(json.encodeToString(settings))
            } catch (e: Exception) {
                println("Error saving settings for device $deviceId: ${e.message}")
            }
        }
    }

    fun loadSettings(deviceId: Long): DeviceServerSettings? {
        val settingsFile = getDeviceSettingsFile(deviceId)
        return lock.withLock {
            try {
                if (settingsFile.exists()) {
                    val fileContent = settingsFile.readText()
                    json.decodeFromString<DeviceServerSettings>(fileContent)
                } else {
                    println("Settings file for device $deviceId does not exist. Creating default settings.")
                    createEmptySettingsFile(deviceId)
                    loadSettings(deviceId)
                }
            } catch (e: Exception) {
                println("Error loading settings for device $deviceId: ${e.message}")
                null
            }
        }
    }
}
