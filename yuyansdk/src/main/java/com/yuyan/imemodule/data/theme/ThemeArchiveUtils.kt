package com.yuyan.imemodule.data.theme

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ThemeArchiveUtils {

    fun exportThemeToYus(context: Context, theme: Theme.Custom, outputUri: Uri) {
        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zipOut ->
                // 1. 写入带有中文注释和 16 进制颜色的 theme.json (包含壁纸坐标描述)
                val encodedJson = CustomThemeSerializer.encodeToChineseAnnotatedHexJson(theme)
                zipOut.putNextEntry(ZipEntry("theme.json"))
                zipOut.write(encodedJson.toByteArray(Charsets.UTF_8))
                zipOut.closeEntry()

                // 2. 写入壁纸原图文件 (src.png)
                theme.backgroundImage?.let { bg ->
                    val srcFile = File(bg.srcFilePath)
                    if (srcFile.exists()) {
                        zipOut.putNextEntry(ZipEntry("src.${srcFile.extension}"))
                        srcFile.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }

                // 3. 写入自定义 Logo 原图文件 (logo.png / logo.gif)
                theme.customLogoFilePath?.let { logoPath ->
                    val logoFile = File(logoPath)
                    if (logoFile.exists()) {
                        zipOut.putNextEntry(ZipEntry("logo.${logoFile.extension}"))
                        logoFile.inputStream().use { it.copyTo(zipOut) }
                        zipOut.closeEntry()
                    }
                }
            }
        }
    }

    fun importThemeFromYus(context: Context, inputUri: Uri): Theme.Custom? {
        val tempDir = File(context.cacheDir, "temp_import_yus_${System.currentTimeMillis()}").apply { mkdirs() }

        var parsedTheme: Theme.Custom? = null
        var tempSrcFile: File? = null
        var tempLogoFile: File? = null

        try {
            context.contentResolver.openInputStream(inputUri)?.use { inputStream ->
                ZipInputStream(inputStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val fileName = entry.name
                        when {
                            fileName == "theme.json" || fileName.endsWith("/theme.json") -> {
                                val baos = java.io.ByteArrayOutputStream()
                                zipIn.copyTo(baos)
                                val rawJson = baos.toString(Charsets.UTF_8.name())
                                val jsonText = CustomThemeSerializer.stripJsonComments(rawJson)
                                val (theme, _) = CustomThemeSerializer.LenientJson.decodeFromString(CustomThemeSerializer.WithMigrationStatus, jsonText)
                                parsedTheme = theme
                            }
                            fileName.startsWith("src.") || fileName.startsWith("background_src.") -> {
                                val ext = File(fileName).extension
                                val dest = File(tempDir, if (ext.isNotEmpty()) "src.$ext" else "src.png")
                                FileOutputStream(dest).use { out -> zipIn.copyTo(out) }
                                tempSrcFile = dest
                            }
                            fileName.startsWith("logo.") -> {
                                val ext = File(fileName).extension
                                val dest = File(tempDir, if (ext.isNotEmpty()) "logo.$ext" else "logo.png")
                                FileOutputStream(dest).use { out -> zipIn.copyTo(out) }
                                tempLogoFile = dest
                            }
                        }
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }
            }

            val baseTheme = parsedTheme ?: return null
            val preferredName = baseTheme.name.ifEmpty { "Theme 1" }
            val finalName = ThemeFilesManager.generateUniqueThemeName(preferredName)
            val targetDir = ThemeFilesManager.getThemeDir(finalName)

            var finalSrcPath = baseTheme.backgroundImage?.srcFilePath
            tempSrcFile?.let { src ->
                if (src.exists()) {
                    val ext = src.extension
                    val dest = File(targetDir, if (ext.isNotEmpty()) "src.$ext" else "src.png")
                    src.copyTo(dest, overwrite = true)
                    finalSrcPath = dest.absolutePath
                }
            }

            var finalLogoPath = baseTheme.customLogoFilePath
            tempLogoFile?.let { logo ->
                if (logo.exists()) {
                    val ext = logo.extension
                    val dest = File(targetDir, if (ext.isNotEmpty()) "logo.$ext" else "logo.png")
                    logo.copyTo(dest, overwrite = true)
                    finalLogoPath = dest.absolutePath
                }
            }

            val newBg = baseTheme.backgroundImage?.let { bg ->
                Theme.Custom.CustomBackground(
                    srcFilePath = finalSrcPath ?: bg.srcFilePath,
                    brightness = bg.brightness,
                    cropRect = bg.cropRect,
                )
            }

            val importedTheme = baseTheme.copy(
                name = finalName,
                backgroundImage = newBg,
                customLogoFilePath = finalLogoPath
            )

            ThemeFilesManager.saveThemeFiles(importedTheme)
            return importedTheme
        } catch (e: Exception) {
            android.util.Log.e("ThemeArchiveUtils", "Error importing yus: ${e.message}", e)
            return null
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
