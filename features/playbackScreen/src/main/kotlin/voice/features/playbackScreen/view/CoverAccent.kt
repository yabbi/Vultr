package voice.features.playbackScreen.view

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.core.ui.ImmutableFile
import voice.core.ui.RavenTheme
import voice.core.ui.theme.LocalRavenColors
import voice.features.playbackScreen.CoverColors

private const val ANALYSIS_SIZE = 64

@Composable
internal fun rememberCoverAccent(cover: ImmutableFile?): Color? {
  val colors = RavenTheme.colors
  val accent by produceState<Color?>(initialValue = null, cover, colors.isDark, colors.bgMain) {
    value = if (cover == null) {
      null
    } else {
      withContext(Dispatchers.IO) { extractAccent(cover, colors.isDark, colors.bgMain.toArgb()) }
    }
  }
  return accent
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
 * artwork. Falls back to the regular theme when the cover has no usable color.
 */
@Composable
internal fun CoverAccentTheme(
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
  CompositionLocalProvider(LocalRavenColors provides themed, content = content)
}
