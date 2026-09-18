package voice.core.ui.theme

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Pure ARGB math so the extraction can be unit tested without an Android runtime.
 */
object CoverColors {

  private const val QUANT_BITS = 4
  private const val QUANT_SHIFT = 8 - QUANT_BITS
  private const val MIN_SATURATION = 0.12f
  // Large-text threshold: trades some label legibility for more vivid accents in light mode.
  private const val MIN_TEXT_CONTRAST = 3f
  private const val MAX_DARK_LUMINANCE = 0.45f
  private const val VIBRANCY_FLOOR = 0.1f

  /**
   * Ranks the image's colors by coverage weighted by vibrancy, drops the ones that would not be
   * readable as text on [background], and returns the best survivor untouched.
   */
  fun accent(
    pixels: IntArray,
    dark: Boolean,
    background: Int,
  ): Int? {
    return rankedColors(pixels)
      .firstOrNull { isUsable(it, dark, background) }
  }

  fun isUsable(
    color: Int,
    dark: Boolean,
    background: Int,
  ): Boolean {
    if (toHsl(color).saturation < MIN_SATURATION) return false
    if (contrast(color, background) < MIN_TEXT_CONTRAST) return false
    if (dark && luminance(color) > MAX_DARK_LUMINANCE) return false
    return true
  }

  /**
   * Colors present in the image, best first. Coverage is weighted by vibrancy so a vivid color
   * beats a muted one unless the muted one is far more common.
   */
  fun rankedColors(pixels: IntArray): List<Int> {
    val bucketCount = 1 shl (QUANT_BITS * 3)
    val counts = IntArray(bucketCount)
    val sumR = LongArray(bucketCount)
    val sumG = LongArray(bucketCount)
    val sumB = LongArray(bucketCount)
    for (pixel in pixels) {
      if (pixel ushr 24 < 0x80) continue
      val r = pixel shr 16 and 0xFF
      val g = pixel shr 8 and 0xFF
      val b = pixel and 0xFF
      val bucket = (r shr QUANT_SHIFT shl (QUANT_BITS * 2)) or
        (g shr QUANT_SHIFT shl QUANT_BITS) or
        (b shr QUANT_SHIFT)
      counts[bucket]++
      sumR[bucket] += r.toLong()
      sumG[bucket] += g.toLong()
      sumB[bucket] += b.toLong()
    }
    return (0 until bucketCount)
      .filter { counts[it] > 0 }
      .map { bucket ->
        val count = counts[bucket]
        val color = rgb(
          (sumR[bucket] / count).toInt(),
          (sumG[bucket] / count).toInt(),
          (sumB[bucket] / count).toInt(),
        )
        color to count * (VIBRANCY_FLOOR + vibrancy(color))
      }
      .sortedByDescending { (_, score) -> score }
      .map { (color, _) -> color }
  }

  /** Chroma: high for vivid mid-tones, near zero for grays, pastels and very dark colors. */
  fun vibrancy(color: Int): Float {
    val hsl = toHsl(color)
    return hsl.saturation * (1f - abs(2f * hsl.lightness - 1f))
  }

  data class Tokens(
    val primary: Int,
    val primaryLight: Int,
    val primaryDark: Int,
    val primaryFaint: Int,
  )

  /** Uses [accent] as-is and derives the related brand tokens from its hue. */
  fun tokens(
    accent: Int,
    dark: Boolean,
  ): Tokens {
    val hsl = toHsl(accent)
    val hue = hsl.hue
    val saturation = hsl.saturation
    return Tokens(
      primary = accent,
      primaryLight = fromHsl(hue, saturation, if (dark) 0.68f else 0.78f),
      primaryDark = fromHsl(hue, saturation, if (dark) 0.85f else 0.22f),
      primaryFaint = fromHsl(hue, saturation.coerceAtMost(0.6f), if (dark) 0.12f else 0.96f),
    )
  }

  fun contrast(
    a: Int,
    b: Int,
  ): Float {
    val la = luminance(a)
    val lb = luminance(b)
    return (max(la, lb) + 0.05f) / (min(la, lb) + 0.05f)
  }

  fun luminance(color: Int): Float {
    fun channel(value: Int): Float {
      val c = value / 255f
      return if (c <= 0.03928f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)
    }
    return 0.2126f * channel(color shr 16 and 0xFF) +
      0.7152f * channel(color shr 8 and 0xFF) +
      0.0722f * channel(color and 0xFF)
  }

  data class Hsl(
    val hue: Float,
    val saturation: Float,
    val lightness: Float,
  )

  fun toHsl(color: Int): Hsl {
    val r = (color shr 16 and 0xFF) / 255f
    val g = (color shr 8 and 0xFF) / 255f
    val b = (color and 0xFF) / 255f
    val maxC = max(r, max(g, b))
    val minC = min(r, min(g, b))
    val delta = maxC - minC
    val lightness = (maxC + minC) / 2f
    if (delta == 0f) return Hsl(0f, 0f, lightness)
    val saturation = delta / (1f - abs(2f * lightness - 1f))
    val hue = when (maxC) {
      r -> 60f * (((g - b) / delta) % 6f)
      g -> 60f * ((b - r) / delta + 2f)
      else -> 60f * ((r - g) / delta + 4f)
    }
    return Hsl(if (hue < 0f) hue + 360f else hue, saturation, lightness)
  }

  fun fromHsl(
    hue: Float,
    saturation: Float,
    lightness: Float,
  ): Int {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val x = c * (1f - abs((hue / 60f) % 2f - 1f))
    val m = lightness - c / 2f
    val (r1, g1, b1) = when {
      hue < 60f -> Triple(c, x, 0f)
      hue < 120f -> Triple(x, c, 0f)
      hue < 180f -> Triple(0f, c, x)
      hue < 240f -> Triple(0f, x, c)
      hue < 300f -> Triple(x, 0f, c)
      else -> Triple(c, 0f, x)
    }
    return rgb(
      ((r1 + m) * 255f).toInt().coerceIn(0, 255),
      ((g1 + m) * 255f).toInt().coerceIn(0, 255),
      ((b1 + m) * 255f).toInt().coerceIn(0, 255),
    )
  }

  private fun rgb(
    r: Int,
    g: Int,
    b: Int,
  ): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
}
