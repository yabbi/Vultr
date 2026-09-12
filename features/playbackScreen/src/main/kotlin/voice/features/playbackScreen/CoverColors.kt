package voice.features.playbackScreen

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Pure ARGB math so the extraction can be unit tested without an Android runtime.
 */
internal object CoverColors {

  private const val QUANT_BITS = 4
  private const val QUANT_SHIFT = 8 - QUANT_BITS
  private const val MIN_SATURATION = 0.12f

  /**
   * Returns the most present color of the given pixels, ignoring near black / white and
   * desaturated buckets. Returns null when the image has no usable color.
   */
  fun accent(pixels: IntArray): Int? {
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

    var best: Int? = null
    var bestScore = 0f
    for (bucket in 0 until bucketCount) {
      val count = counts[bucket]
      if (count == 0) continue
      val color = rgb(
        (sumR[bucket] / count).toInt(),
        (sumG[bucket] / count).toInt(),
        (sumB[bucket] / count).toInt(),
      )
      val hsl = toHsl(color)
      if (hsl.lightness < 0.08f || hsl.lightness > 0.92f) continue
      if (hsl.saturation < MIN_SATURATION) continue
      // Frequency dominates, saturation breaks ties towards vivid colors.
      val score = count * (0.25f + hsl.saturation)
      if (score > bestScore) {
        bestScore = score
        best = color
      }
    }
    return best
  }

  data class Tokens(
    val primary: Int,
    val primaryLight: Int,
    val primaryDark: Int,
    val primaryFaint: Int,
  )

  /**
   * Normalizes the accent so it reads as text against [background] and derives the related brand
   * tokens. HSL lightness alone is not enough: yellows and cyans at medium lightness are still
   * nearly white, so the final check is a WCAG contrast ratio.
   */
  fun tokens(
    accent: Int,
    dark: Boolean,
    background: Int,
  ): Tokens {
    val hsl = toHsl(accent)
    val hue = hsl.hue
    val saturation = hsl.saturation.coerceIn(0.4f, 0.9f)
    var lightness = hsl.lightness.coerceIn(0.42f, 0.6f)
    if (dark) {
      while (lightness > MIN_LIGHTNESS && luminance(fromHsl(hue, saturation, lightness)) > MAX_DARK_LUMINANCE) {
        lightness -= LIGHTNESS_STEP
      }
      while (lightness < MAX_LIGHTNESS && contrast(fromHsl(hue, saturation, lightness), background) < MIN_TEXT_CONTRAST) {
        lightness += LIGHTNESS_STEP
      }
    } else {
      while (lightness > MIN_LIGHTNESS && contrast(fromHsl(hue, saturation, lightness), background) < MIN_TEXT_CONTRAST) {
        lightness -= LIGHTNESS_STEP
      }
    }
    return Tokens(
      primary = fromHsl(hue, saturation, lightness),
      primaryLight = fromHsl(hue, saturation, if (dark) 0.68f else 0.78f),
      primaryDark = fromHsl(hue, saturation, if (dark) 0.85f else 0.22f),
      primaryFaint = fromHsl(hue, saturation.coerceAtMost(0.6f), if (dark) 0.12f else 0.96f),
    )
  }

  private const val MIN_TEXT_CONTRAST = 4.5f
  private const val MAX_DARK_LUMINANCE = 0.45f
  private const val LIGHTNESS_STEP = 0.02f
  private const val MIN_LIGHTNESS = 0.15f
  private const val MAX_LIGHTNESS = 0.85f

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
