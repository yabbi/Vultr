package voice.core.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.animation.animateColorAsState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.core.ui.ImmutableFile
import voice.core.ui.RavenTheme
import voice.core.ui.darkSchemeFrom
import voice.core.ui.lightSchemeFrom
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

private const val ANALYSIS_SIZE = 64
private const val CACHE_SIZE = 64

// Process-wide so a cover analyzed on one screen resolves synchronously on the next; without it
// every screen would first render with the default brand color and then jump to the accent.
private val accentCache = LruCache<AccentKey, Optional<Color>>(CACHE_SIZE)

private data class AccentKey(
  val path: String,
  val lastModified: Long,
  val dark: Boolean,
  val background: Int,
)

private fun ImmutableFile.accentKey(
  dark: Boolean,
  background: Int,
) = AccentKey(file.absolutePath, file.lastModified(), dark, background)

@Composable
fun rememberCoverAccent(cover: ImmutableFile?): Color? {
  val colors = RavenTheme.colors
  val background = colors.bgMain.toArgb()
  val cached = remember(cover, colors.isDark, background) {
    cover?.let { accentCache[it.accentKey(colors.isDark, background)] }
  }
  val accent by produceState(initialValue = cached?.getOrNull(), cover, colors.isDark, background) {
    value = if (cover == null) null else cachedAccent(cover, colors.isDark, background)
  }
  return accent
}

/** Analyzes [cover] ahead of time so screens opened for it later start with the accent applied. */
@Composable
fun PrewarmCoverAccent(cover: ImmutableFile?) {
  if (cover == null) return
  val colors = RavenTheme.colors
  val background = colors.bgMain.toArgb()
  LaunchedEffect(cover, colors.isDark, background) {
    warmAccent(cover, colors.isDark, background)
  }
}

private suspend fun warmAccent(
  cover: ImmutableFile,
  dark: Boolean,
  background: Int,
) {
  val key = cover.accentKey(dark, background)
  if (accentCache[key] != null) return
  val accent = withContext(Dispatchers.IO) { extractAccent(cover, dark, background) }
  accentCache.put(key, Optional.ofNullable(accent))
}

private suspend fun cachedAccent(
  cover: ImmutableFile,
  dark: Boolean,
  background: Int,
): Color? {
  warmAccent(cover, dark, background)
  return accentCache[cover.accentKey(dark, background)]?.getOrNull()
}

private fun extractAccent(
  cover: ImmutableFile,
  dark: Boolean,
  background: Int,
): Color? {
  val bitmap = decodeSmall(cover) ?: return null
  return try {
    val analysis = Bitmap.createScaledBitmap(bitmap, ANALYSIS_SIZE, ANALYSIS_SIZE, true)
    val pixels = IntArray(ANALYSIS_SIZE * ANALYSIS_SIZE)
    analysis.getPixels(pixels, 0, ANALYSIS_SIZE, 0, 0, ANALYSIS_SIZE, ANALYSIS_SIZE)
    CoverColors.accent(pixels, dark, background)?.let(::Color)
  } catch (e: Exception) {
    null
  }
}

private fun decodeSmall(cover: ImmutableFile): Bitmap? {
  val path = cover.file.absolutePath
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeFile(path, bounds)
  if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
  var sampleSize = 1
  while (bounds.outWidth / (sampleSize * 2) >= ANALYSIS_SIZE * 2 &&
    bounds.outHeight / (sampleSize * 2) >= ANALYSIS_SIZE * 2
  ) {
    sampleSize *= 2
  }
  val options = BitmapFactory.Options().apply {
    inSampleSize = sampleSize
    inPreferredConfig = Bitmap.Config.ARGB_8888
  }
  return BitmapFactory.decodeFile(path, options)
}

/**
 * Re-themes the brand tokens from the cover accent so every control using `primary` follows the
 * artwork. Both the Raven tokens and the Material color scheme are re-derived, so Material
 * components such as progress indicators pick up the accent as well. Falls back to the regular
 * theme when the cover has no usable color.
 */
@Composable
fun CoverAccentTheme(
  accent: Color?,
  content: @Composable () -> Unit,
) {
  val base = RavenTheme.colors
  val tokens = accent?.let { CoverColors.tokens(it.toArgb(), base.isDark) }
  val primary by animateColorAsState(tokens?.primary?.let(::Color) ?: base.primary, label = "accent")
  val primaryLight by animateColorAsState(tokens?.primaryLight?.let(::Color) ?: base.primaryLight, label = "accentLight")
  val primaryDark by animateColorAsState(tokens?.primaryDark?.let(::Color) ?: base.primaryDark, label = "accentDark")
  val primaryFaint by animateColorAsState(tokens?.primaryFaint?.let(::Color) ?: base.primaryFaint, label = "accentFaint")
  val themed = base.copy(
    primary = primary,
    primaryLight = primaryLight,
    primaryDark = primaryDark,
    primaryFaint = primaryFaint,
  )
  val scheme = if (themed.isDark) darkSchemeFrom(themed) else lightSchemeFrom(themed)
  CompositionLocalProvider(LocalRavenColors provides themed) {
    MaterialTheme(
      colorScheme = scheme,
      typography = MaterialTheme.typography,
      shapes = MaterialTheme.shapes,
      content = content,
    )
  }
}
