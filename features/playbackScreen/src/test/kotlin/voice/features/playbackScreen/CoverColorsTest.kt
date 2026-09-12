package voice.features.playbackScreen

import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class CoverColorsTest {

  private fun pixels(vararg colors: Pair<Int, Int>): IntArray {
    return colors.flatMap { (color, count) -> List(count) { color } }.toIntArray()
  }

  @Test
  fun `most present saturated color wins`() {
    val accent = CoverColors.accent(pixels(0xFFC0392B.toInt() to 500, 0xFF2980B9.toInt() to 100))
    val hsl = CoverColors.toHsl(accent.shouldNotBeNull())
    hsl.hue.shouldBeLessThan(15f)
  }

  @Test
  fun `near black and white are ignored`() {
    val accent = CoverColors.accent(pixels(0xFF050505.toInt() to 800, 0xFFFAFAFA.toInt() to 800, 0xFF27AE60.toInt() to 10))
    val hsl = CoverColors.toHsl(accent.shouldNotBeNull())
    hsl.hue.shouldBeGreaterThan(120f)
    hsl.hue.shouldBeLessThan(160f)
  }

  @Test
  fun `grayscale cover yields no accent`() {
    CoverColors.accent(pixels(0xFF808080.toInt() to 400, 0xFF404040.toInt() to 400)).shouldBeNull()
  }

  @Test
  fun `transparent pixels are skipped`() {
    CoverColors.accent(pixels(0x10FF0000 to 400)).shouldBeNull()
  }

  @Test
  fun `tokens keep the hue and clamp lightness for readable white content`() {
    val tokens = CoverColors.tokens(0xFFFFF176.toInt(), dark = false)
    val hsl = CoverColors.toHsl(tokens.primary)
    hsl.hue.shouldBeGreaterThan(45f)
    hsl.hue.shouldBeLessThan(60f)
    hsl.lightness.shouldBeLessThan(0.61f)
    hsl.lightness.shouldBeGreaterThan(0.41f)
  }

  @Test
  fun `hsl round trips`() {
    CoverColors.fromHsl(0f, 0f, 0.5f) shouldBe 0xFF7F7F7F.toInt()
    val hsl = CoverColors.toHsl(0xFF457CFA.toInt())
    val back = CoverColors.toHsl(CoverColors.fromHsl(hsl.hue, hsl.saturation, hsl.lightness))
    back.hue.shouldBeGreaterThan(hsl.hue - 1f)
    back.hue.shouldBeLessThan(hsl.hue + 1f)
  }
}
