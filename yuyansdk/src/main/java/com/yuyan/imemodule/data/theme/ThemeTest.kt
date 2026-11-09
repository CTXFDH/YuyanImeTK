package com.yuyan.imemodule.data.theme

import java.io.File
import java.util.zip.ZipInputStream

fun main() {
    val themeDir = File("C:/Users/wen/AndroidStudioProjects/YuyanImeK-master/theme")
    themeDir.listFiles()?.forEach { file ->
        if (file.extension == "yus") {
            println("Testing: ${file.name}")
            try {
                ZipInputStream(file.inputStream()).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        if (entry.name == "theme.json") {
                            val baos = java.io.ByteArrayOutputStream()
                            zipIn.copyTo(baos)
                            val rawJson = baos.toString(Charsets.UTF_8.name())
                            val jsonText = CustomThemeSerializer.stripJsonComments(rawJson)
                            val (theme, _) = CustomThemeSerializer.LenientJson.decodeFromString(CustomThemeSerializer.WithMigrationStatus, jsonText)
                            println("  SUCCESS parsing ${theme.name}")
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            } catch (e: Exception) {
                println("  FAILED parsing ${file.name}: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}
