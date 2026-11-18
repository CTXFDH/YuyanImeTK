package com.yuyan.imemodule.utils

import java.io.File
import java.util.zip.ZipInputStream

/**
 * @return top-level files in zip file
 */
fun ZipInputStream.extract(destDir: File): List<File> {
    var entry = nextEntry
    val canonicalDest = if (destDir.canonicalPath.endsWith(File.separator)) destDir.canonicalPath else destDir.canonicalPath + File.separator
    while (entry != null) {
        val targetFile = File(destDir, entry.name)
        val canonicalTarget = targetFile.canonicalPath
        if (!canonicalTarget.startsWith(canonicalDest) && canonicalTarget != destDir.canonicalPath) {
            throw SecurityException("Zip Slip detected: ${entry.name}")
        }
        if (!entry.isDirectory) {
            targetFile.parentFile?.mkdirs()
            targetFile.outputStream().use { out -> copyTo(out) }
        } else {
            targetFile.mkdirs()
        }
        entry = nextEntry
    }
    return destDir.listFiles()?.toList() ?: emptyList()
}
