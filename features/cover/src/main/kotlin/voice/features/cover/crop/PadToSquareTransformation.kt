package voice.features.cover.crop

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.ColorInt
import coil.size.Size
import coil.transform.Transformation
import kotlin.math.max

class PadToSquareTransformation(
  @ColorInt private val backgroundColor: Int,
) : Transformation {

  override val cacheKey: String = "padToSquareTransformation($backgroundColor)"

  override suspend fun transform(
    input: Bitmap,
    size: Size,
  ): Bitmap {
    if (input.width == input.height) return input
    val side = max(input.width, input.height)
    val output = Bitmap.createBitmap(side, side, input.config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    canvas.drawColor(backgroundColor)
    val left = (side - input.width) / 2f
    val top = (side - input.height) / 2f
    canvas.drawBitmap(input, left, top, null)
    return output
  }
}
