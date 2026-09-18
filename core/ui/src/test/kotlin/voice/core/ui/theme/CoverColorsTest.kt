package voice.core.ui.theme

import io.kotest.matchers.floats.shouldBeGreaterThan
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.junit.Test

class CoverColorsTest {

  private companion object {
    val WHITE = 0xFFFFFFFF.toInt()
    val NEAR_BLACK = 0xFF00030A.toInt()
    val RED = 0xFFC0392B.toInt()
    val BLUE = 0xFF2980B9.toInt()
    val GREEN = 0xFF27AE60.toInt()
    val YELLOW = 0xFFFFF176.toInt()
    val DARK_PURPLE = 0xFF3B0A45.toInt()
    val NAVY = 0xFF1F4E9C.toInt()
    val ORANGE = 0xFFC88738.toInt()
    val TAUPE = 0xFF6E6248.toInt()
  }

  private fun pixels(vararg colors: Pair<Int, Int>): IntArray {
    return colors.flatMap { (color, count) -> List(count) { color } }.toIntArray()
  }

  @Test
  fun `most present usable color wins`() {
    CoverColors.accent(pixels(RED to 500, BLUE to 100), dark = false, background = WHITE) shouldBe RED
  }

  @Test
  fun `the chosen color is returned unchanged`() {
    val accent = CoverColors.accent(pixels(GREEN to 10), dark = true, background = NEAR_BLACK)
    accent shouldBe GREEN
    CoverColors.tokens(GREEN, dark = true).primary shouldBe GREEN
  }

  @Test
  fun `near black and white are skipped for a less common usable color`() {
    val accent = CoverColors.accent(
      pixels(0xFF050505.toInt() to 800, 0xFFFAFAFA.toInt() to 800, NAVY to 10),
      dark = false,
      background = WHITE,
    )
    accent shouldBe NAVY
  }

  @Test
  fun `bright yellow is skipped on a light background in favor of a readable color`() {
    val accent = CoverColors.accent(pixels(YELLOW to 900, NAVY to 50), dark = false, background = WHITE)
    accent shouldBe NAVY
  }

  @Test
  fun `a mid-tone orange passes the large-text threshold on white`() {
    CoverColors.accent(pixels(ORANGE to 100), dark = false, background = WHITE) shouldBe ORANGE
  }

  @Test
  fun `bright yellow is skipped on a dark background as too glaring`() {
    val accent = CoverColors.accent(pixels(YELLOW to 900, BLUE to 50), dark = true, background = NEAR_BLACK)
    accent shouldBe BLUE
  }

  @Test
  fun `dark colors are skipped on a dark background`() {
    val accent = CoverColors.accent(pixels(DARK_PURPLE to 900, BLUE to 50), dark = true, background = NEAR_BLACK)
    accent shouldBe BLUE
  }

  @Test
  fun `grayscale cover yields no accent`() {
    CoverColors.accent(pixels(0xFF808080.toInt() to 400, 0xFF404040.toInt() to 400), dark = false, background = WHITE)
      .shouldBeNull()
  }

  @Test
  fun `nothing usable yields no accent`() {
    CoverColors.accent(pixels(YELLOW to 400), dark = false, background = WHITE).shouldBeNull()
  }

  @Test
  fun `transparent pixels are skipped`() {
    CoverColors.accent(pixels(0x10FF0000 to 400), dark = false, background = WHITE).shouldBeNull()
  }

  @Test
  fun `equally vibrant colors are ordered by pixel count`() {
    CoverColors.rankedColors(pixels(BLUE to 100, RED to 300, GREEN to 200)) shouldBe listOf(RED, GREEN, BLUE)
  }

  @Test
  fun `a vivid color beats a more common muted one`() {
    CoverColors.accent(pixels(TAUPE to 600, NAVY to 400), dark = false, background = WHITE) shouldBe NAVY
  }

  @Test
  fun `a muted color still wins when it dominates the image`() {
    CoverColors.accent(pixels(TAUPE to 950, NAVY to 50), dark = false, background = WHITE) shouldBe TAUPE
  }

  @Test
  fun `a tiny usable color is still picked when nothing else passes`() {
    CoverColors.accent(pixels(YELLOW to 990, NAVY to 10), dark = false, background = WHITE) shouldBe NAVY
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
