package com.getwinharris.bapxcut.utils

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import java.io.File
import java.util.Locale

data class FontItem(
    val name: String,
    val path: String,
    val isCustom: Boolean,
    val typeface: Typeface? = null
)

object FontManager {
    private const val TAG = "FontManager"
    private const val CUSTOM_FONTS_DIR = "custom_fonts"

    fun getCustomFontsDir(context: Context): File {
        val dir = File(context.filesDir, CUSTOM_FONTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun importFontFromUri(context: Context, uri: Uri): FontItem? {
        return try {
            val fileName = getFileNameFromUri(context, uri) ?: "Font_${System.currentTimeMillis()}.ttf"
            val sanitizedName = sanitizeFileName(fileName)
            val destinationDir = getCustomFontsDir(context)
            val destFile = File(destinationDir, sanitizedName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                destFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            val typeface = try {
                Typeface.createFromFile(destFile)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load typeface from imported font: ${destFile.absolutePath}", e)
                destFile.delete()
                return null
            }

            val fontName = destFile.nameWithoutExtension
                .replace("_", " ")
                .replace("-", " ")

            FontItem(
                name = fontName,
                path = destFile.absolutePath,
                isCustom = true,
                typeface = typeface
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to import font from URI: $uri", e)
            null
        }
    }

    fun getCustomFonts(context: Context): List<FontItem> {
        val dir = getCustomFontsDir(context)
        val files = dir.listFiles() ?: return emptyList()
        return files.filter { file ->
            file.isFile && file.extension.lowercase(Locale.US) in setOf("ttf", "otf")
        }.sortedBy { it.nameWithoutExtension.lowercase(Locale.US) }
        .mapNotNull { file ->
            val tf = try {
                Typeface.createFromFile(file)
            } catch (e: Exception) {
                null
            }
            if (tf != null) {
                FontItem(
                    name = file.nameWithoutExtension.replace("_", " ").replace("-", " "),
                    path = file.absolutePath,
                    isCustom = true,
                    typeface = tf
                )
            } else null
        }
    }

    fun getAllFonts(context: Context, defaultFontPath: String?): List<FontItem> {
        val fonts = mutableListOf<FontItem>()
        val pathsSeen = mutableSetOf<String>()

        // 1. Default bundled font (Roboto Regular)
        if (!defaultFontPath.isNullOrBlank() && File(defaultFontPath).exists()) {
            val defaultTf = try { Typeface.createFromFile(defaultFontPath) } catch (e: Exception) { Typeface.DEFAULT }
            fonts.add(FontItem("Roboto Regular", defaultFontPath, isCustom = false, typeface = defaultTf))
            pathsSeen.add(defaultFontPath)
        }

        // 2. User-imported custom fonts
        getCustomFonts(context).forEach { item ->
            if (!pathsSeen.contains(item.path)) {
                fonts.add(item)
                pathsSeen.add(item.path)
            }
        }

        // 3. System fonts
        val fontDirectories = listOf(
            File("/system/fonts"),
            File("/data/fonts"),
            File("/product/fonts"),
            File(context.cacheDir, "fonts")
        )

        for (dir in fontDirectories) {
            if (!dir.exists() || !dir.isDirectory) continue
            dir.listFiles()
                ?.filter { file -> file.isFile && file.extension.lowercase(Locale.US) in setOf("ttf", "otf") }
                ?.sortedBy { it.nameWithoutExtension.lowercase(Locale.US) }
                ?.forEach { file ->
                    if (!pathsSeen.contains(file.absolutePath)) {
                        val tf = try { Typeface.createFromFile(file) } catch (e: Exception) { null }
                        if (tf != null) {
                            fonts.add(FontItem(
                                name = file.nameWithoutExtension,
                                path = file.absolutePath,
                                isCustom = false,
                                typeface = tf
                            ))
                            pathsSeen.add(file.absolutePath)
                        }
                    }
                }
        }

        if (fonts.isEmpty() && !defaultFontPath.isNullOrBlank()) {
            fonts.add(FontItem("Roboto Regular", defaultFontPath, isCustom = false, typeface = Typeface.DEFAULT))
        }

        return fonts
    }

    fun deleteCustomFont(context: Context, fontPath: String): Boolean {
        return try {
            val file = File(fontPath)
            if (file.exists() && file.parentFile?.name == CUSTOM_FONTS_DIR) {
                file.delete()
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting custom font: $fontPath", e)
            false
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            }
        }
        if (name == null) {
            name = uri.path?.let { File(it).name }
        }
        return name
    }

    private fun sanitizeFileName(name: String): String {
        val ext = if (name.endsWith(".otf", ignoreCase = true)) ".otf" else ".ttf"
        val baseName = name.substringBeforeLast(".").replace(Regex("[^a-zA-Z0-9_-]"), "_")
        return "$baseName$ext"
    }
}
