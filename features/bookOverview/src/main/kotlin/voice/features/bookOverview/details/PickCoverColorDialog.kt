package voice.features.bookOverview.details

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import voice.core.ui.ImmutableFile
import voice.core.ui.RavenTheme
import voice.core.ui.theme.rememberCoverAccent
import java.io.File

private const val MAX_DECODE_SIZE = 1024

@Composable
internal fun PickCoverColorDialog(
  cover: ImmutableFile,
  currentOverride: Int?,
  onConfirm: (Int) -> Unit,
  onReset: () -> Unit,
  onDismiss: () -> Unit,
) {
  val extracted = rememberCoverAccent(cover)
  var bitmap by remember { mutableStateOf<Bitmap?>(null) }
  var picked by remember { mutableStateOf<Int?>(null) }
  var pickedFraction by remember { mutableStateOf<Offset?>(null) }
  var resetPending by remember { mutableStateOf(false) }
  LaunchedEffect(cover) {
    bitmap = withContext(Dispatchers.IO) { decodeForPicking(cover.file) }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Override Control Color") },
    text = {
      Column {
        Text(
          text = "Touch the cover to pick the color for buttons and progress.",
          fontSize = 14.sp,
          color = RavenTheme.colors.subTitle,
        )
        Spacer(Modifier.height(12.dp))
        val image = bitmap
        if (image != null) {
          ColorPickingCover(
            bitmap = image,
            marker = pickedFraction,
            onPick = { fraction ->
              resetPending = false
              pickedFraction = fraction
              picked = image.colorAt(fraction)
            },
          )
          Spacer(Modifier.height(12.dp))
          PickedColorPreview(
            label = when {
              resetPending -> "Default"
              picked != null -> "Selected"
              else -> "Current"
            },
            color = when {
              resetPending -> extracted?.toArgb()
              picked != null -> picked
              else -> currentOverride ?: extracted?.toArgb()
            },
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        enabled = picked != null || resetPending,
        onClick = {
          if (resetPending) onReset() else picked?.let(onConfirm)
        },
      ) {
        Text("Set")
      }
    },
    dismissButton = {
      Row {
        TextButton(
          enabled = currentOverride != null && !resetPending,
          onClick = {
            resetPending = true
            picked = null
            pickedFraction = null
          },
        ) {
          Text("Reset")
        }
        TextButton(onClick = onDismiss) {
          Text("Cancel")
        }
      }
    },
  )
}

@Composable
private fun ColorPickingCover(
  bitmap: Bitmap,
  marker: Offset?,
  onPick: (Offset) -> Unit,
) {
  val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
      .pointerInput(bitmap) {
        detectTapGestures { position -> onPick(position.toFraction(size.width, size.height)) }
      }
      .pointerInput(bitmap) {
        detectDragGestures { change, _ ->
          change.consume()
          onPick(change.position.toFraction(size.width, size.height))
        }
      },
  ) {
    Image(
      bitmap = imageBitmap,
      contentDescription = "Cover",
      modifier = Modifier.fillMaxWidth(),
      contentScale = ContentScale.FillBounds,
    )
    if (marker != null) {
      Canvas(Modifier.matchParentSize()) {
        val center = Offset(marker.x * size.width, marker.y * size.height)
        drawCircle(Color.Black, radius = 14.dp.toPx(), center = center, style = Stroke(3.dp.toPx()))
        drawCircle(Color.White, radius = 12.dp.toPx(), center = center, style = Stroke(2.dp.toPx()))
      }
    }
  }
}

@Composable
private fun PickedColorPreview(
  label: String,
  color: Int?,
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(32.dp)
        .clip(CircleShape)
        .background(color?.let(::Color) ?: RavenTheme.colors.borderAvg),
    )
    Spacer(Modifier.width(12.dp))
    Text(
      text = color?.let { "$label: #%06X".format(it and 0xFFFFFF) } ?: "$label: default",
      fontSize = 14.sp,
      color = RavenTheme.colors.subTitle,
    )
  }
}

private fun Offset.toFraction(
  width: Int,
  height: Int,
) = Offset(
  (x / width).coerceIn(0f, 1f),
  (y / height).coerceIn(0f, 1f),
)

private fun Bitmap.colorAt(fraction: Offset): Int {
  val px = (fraction.x * width).toInt().coerceIn(0, width - 1)
  val py = (fraction.y * height).toInt().coerceIn(0, height - 1)
  return getPixel(px, py) or (0xFF shl 24)
}

private fun decodeForPicking(cover: File): Bitmap? {
  val path = cover.absolutePath
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeFile(path, bounds)
  if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
  var sampleSize = 1
  while (bounds.outWidth / (sampleSize * 2) >= MAX_DECODE_SIZE &&
    bounds.outHeight / (sampleSize * 2) >= MAX_DECODE_SIZE
  ) {
    sampleSize *= 2
  }
  val options = BitmapFactory.Options().apply {
    inSampleSize = sampleSize
    inPreferredConfig = Bitmap.Config.ARGB_8888
  }
  return BitmapFactory.decodeFile(path, options)
}
