package com.zeuroux.launchly.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlin.math.min
import kotlin.math.sqrt

class InvalidUserImageException(message: String) : Exception(message)

class UserImageStore(private val context: Context) {
    suspend fun storeIcon(versionId: String, uri: Uri): String = withContext(Dispatchers.IO) {
        runCatching { UUID.fromString(versionId) }
            .getOrElse { throw InvalidUserImageException("The managed version identifier is invalid.") }
        val versionsRoot = File(context.filesDir, "versions").canonicalFile.apply { mkdirs() }
        val versionDirectory = File(versionsRoot, versionId).canonicalFile
        if (versionDirectory.parentFile != versionsRoot) throw InvalidUserImageException("The icon path is invalid.")
        versionDirectory.mkdirs()

        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val bitmap = try {
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val width = info.size.width
                val height = info.size.height
                if (width <= 0 || height <= 0) throw InvalidUserImageException("The selected image has invalid dimensions.")
                val pixelCount = width.toLong() * height.toLong()
                if (pixelCount > MAX_INPUT_PIXELS) throw InvalidUserImageException("The selected image is too large.")
                val dimensionScale = min(1.0, MAX_OUTPUT_DIMENSION.toDouble() / maxOf(width, height).toDouble())
                val pixelScale = min(1.0, sqrt(MAX_OUTPUT_PIXELS.toDouble() / pixelCount.toDouble()))
                val scale = min(dimensionScale, pixelScale)
                decoder.setTargetSize(
                    (width * scale).toInt().coerceAtLeast(1),
                    (height * scale).toInt().coerceAtLeast(1)
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = false
            }
        } catch (failure: InvalidUserImageException) {
            throw failure
        } catch (_: Exception) {
            throw InvalidUserImageException("The selected file is not a supported image.")
        }

        val destination = File(versionDirectory, "custom_icon.png")
        val temporary = File(versionDirectory, "custom_icon.png.tmp")
        try {
            temporary.outputStream().buffered().use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                    throw InvalidUserImageException("The selected image could not be encoded.")
                }
            }
            Files.move(
                temporary.toPath(), destination.toPath(),
                StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE
            )
        } finally {
            temporary.delete()
            bitmap.recycle()
        }
        destination.absolutePath
    }

    companion object {
        private const val MAX_INPUT_PIXELS = 40_000_000L
        private const val MAX_OUTPUT_PIXELS = 1_048_576L
        private const val MAX_OUTPUT_DIMENSION = 1024
    }
}
