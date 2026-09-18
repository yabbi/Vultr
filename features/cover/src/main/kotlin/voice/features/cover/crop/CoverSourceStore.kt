package voice.features.cover.crop

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import voice.core.data.BookId
import voice.core.logging.api.Logger
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/**
 * Keeps the uncropped source image and the last crop per book so the crop can be adjusted later
 * without losing content.
 */
class CoverSourceStore(private val context: Context) {

  @Serializable
  data class StoredCrop(
    val coverPath: String,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
  ) {
    fun fractions(): RectF = RectF(left, top, right, bottom)
  }

  data class Source(
    val original: File,
    val crop: StoredCrop,
  )

  private val dir: File get() = File(context.filesDir, "coverSources")

  private fun key(bookId: BookId): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(bookId.value.toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
  }

  private fun originalFile(bookId: BookId) = File(dir, "${key(bookId)}.original")
  private fun cropFile(bookId: BookId) = File(dir, "${key(bookId)}.json")

  suspend fun load(bookId: BookId): Source? = withContext(Dispatchers.IO) {
    val original = originalFile(bookId)
    val cropFile = cropFile(bookId)
    if (!original.exists() || !cropFile.exists()) return@withContext null
    try {
      Source(original, Json.decodeFromString<StoredCrop>(cropFile.readText()))
    } catch (e: Exception) {
      Logger.w(e, "Could not read stored crop for $bookId")
      null
    }
  }

  suspend fun save(
    bookId: BookId,
    source: Uri,
    fractions: RectF,
    savedCover: File,
  ) = withContext(Dispatchers.IO) {
    dir.mkdirs()
    val original = originalFile(bookId)
    try {
      if (source != Uri.fromFile(original)) {
        context.contentResolver.openInputStream(source)?.use { input ->
          original.outputStream().use { input.copyTo(it) }
        } ?: return@withContext
      }
      val crop = StoredCrop(
        coverPath = savedCover.absolutePath,
        left = fractions.left,
        top = fractions.top,
        right = fractions.right,
        bottom = fractions.bottom,
      )
      cropFile(bookId).writeText(Json.encodeToString(crop))
    } catch (e: IOException) {
      Logger.w(e, "Could not store cover source for $bookId")
    }
  }
}
