package voice.features.cover.crop

import android.graphics.Color
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.DialogSceneStrategy
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.launch
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.scanner.CoverSaver
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.Navigator
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface EditCoverComponent {
  val coverSaver: CoverSaver
}

@ContributesTo(AppScope::class)
interface EditCoverDialogProvider {

  @Provides
  @IntoSet
  fun editCoverDialogNavEntryProvider(navigator: Navigator): NavEntryProvider<*> = NavEntryProvider<Destination.EditCover> { key ->
    NavEntry(key, metadata = DialogSceneStrategy.dialog()) {
      EditCoverDialog(
        coverUri = key.cover,
        bookId = key.bookId,
        adjustExisting = key.adjustExisting,
        onDismiss = navigator::goBack,
      )
    }
  }
}

@Composable
fun EditCoverDialog(
  coverUri: Uri,
  bookId: BookId,
  adjustExisting: Boolean,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val sourceStore = remember { CoverSourceStore(context) }

  var source by remember { mutableStateOf<CropSource?>(null) }
  LaunchedEffect(coverUri, adjustExisting) {
    val stored = if (adjustExisting) sourceStore.load(bookId) else null
    source = if (stored != null && stored.crop.coverPath == coverUri.path) {
      CropSource(Uri.fromFile(stored.original), stored.crop.fractions())
    } else {
      CropSource(coverUri, null)
    }
  }
  val resolved = source ?: return

  CropDialog(
    source = resolved,
    onConfirm = { overlay ->
      scope.launch {
        val fractions = overlay.selectionFractions
        val bitmap = context.imageLoader
          .execute(
            ImageRequest.Builder(context)
              .data(resolved.uri)
              .transformations(
                CropTransformation(
                  cropOverlay = overlay,
                  sourceWidth = overlay.width,
                  sourceHeight = overlay.height,
                ),
                PadToSquareTransformation(Color.BLACK),
              )
              .build(),
          )
          .drawable?.toBitmap()

        if (bitmap != null) {
          val saved = rootGraphAs<EditCoverComponent>().coverSaver.save(bookId, bitmap)
          sourceStore.save(bookId, resolved.uri, fractions, saved)
        }
        onDismiss()
      }
    },
    onDismiss = onDismiss,
  )
}

private data class CropSource(
  val uri: Uri,
  val initialSelection: RectF?,
)

@Composable
private fun CropDialog(
  source: CropSource,
  onConfirm: (CropOverlay) -> Unit,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val coverUri = source.uri

  var cropOverlay: CropOverlay? by remember { mutableStateOf(null) }
  var imageWidth by remember { mutableIntStateOf(0) }
  var imageHeight by remember { mutableIntStateOf(0) }
  var selectedRect by remember { mutableStateOf<Rect?>(null) }
  var sourceBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
  var squareLocked by remember { mutableStateOf(false) }

  LaunchedEffect(coverUri) {
    sourceBitmap = context.imageLoader
      .execute(ImageRequest.Builder(context).data(coverUri).build())
      .drawable?.toBitmap()?.asImageBitmap()
  }

  AlertDialog(
    onDismissRequest = { onDismiss() },
    title = { Text(text = "Crop Cover") },
    text = {
      Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box {
          AsyncImage(
            model = coverUri,
            contentDescription = stringResource(StringsR.string.content_cover),
            modifier = Modifier
              .fillMaxWidth()
              .onSizeChanged {
                imageWidth = it.width
                imageHeight = it.height
              },
          )

          if (imageWidth > 0 && imageHeight > 0) {
            AndroidView(
              modifier = Modifier
                .width(with(LocalDensity.current) { imageWidth.toDp() })
                .height(with(LocalDensity.current) { imageHeight.toDp() }),
              factory = { ctx ->
                CropOverlay(ctx).apply {
                  onSelectionChanged = { selectedRect = it }
                  source.initialSelection?.let { selectionFractions = it }
                  selectionOn = true
                  lockSquare = squareLocked
                  cropOverlay = this
                }
              },
              update = { it.lockSquare = squareLocked },
            )
          }
        }

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { squareLocked = !squareLocked },
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Checkbox(checked = squareLocked, onCheckedChange = { squareLocked = it })
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "Square")
        }

        val bitmap = sourceBitmap
        val rect = selectedRect
        if (bitmap != null && rect != null && !rect.isEmpty && imageWidth > 0 && imageHeight > 0) {
          CropPreview(
            bitmap = bitmap,
            selection = rect,
            overlayWidth = imageWidth,
            overlayHeight = imageHeight,
            modifier = Modifier.padding(top = 16.dp),
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          val overlay = cropOverlay
          if (overlay != null && !overlay.selectedRect.isEmpty) {
            onConfirm(overlay)
          } else {
            onDismiss()
          }
        },
      ) {
        Text(text = "Set")
      }
    },
    dismissButton = {
      TextButton(onClick = { onDismiss() }) {
        Text(text = "Cancel")
      }
    },
  )
}


@Composable
private fun CropPreview(
  bitmap: ImageBitmap,
  selection: Rect,
  overlayWidth: Int,
  overlayHeight: Int,
  modifier: Modifier = Modifier,
) {
  Canvas(
    modifier = modifier
      .size(140.dp)
      .background(androidx.compose.ui.graphics.Color.Black),
  ) {
    val scaleX = bitmap.width.toFloat() / overlayWidth
    val scaleY = bitmap.height.toFloat() / overlayHeight
    val srcLeft = (selection.left * scaleX).toInt().coerceIn(0, bitmap.width)
    val srcTop = (selection.top * scaleY).toInt().coerceIn(0, bitmap.height)
    val srcRight = (selection.right * scaleX).toInt().coerceIn(srcLeft, bitmap.width)
    val srcBottom = (selection.bottom * scaleY).toInt().coerceIn(srcTop, bitmap.height)
    val srcWidth = srcRight - srcLeft
    val srcHeight = srcBottom - srcTop
    if (srcWidth <= 0 || srcHeight <= 0) return@Canvas

    val side = size.minDimension
    val scale = side / maxOf(srcWidth, srcHeight)
    val dstWidth = (srcWidth * scale).toInt()
    val dstHeight = (srcHeight * scale).toInt()
    drawImage(
      image = bitmap,
      srcOffset = IntOffset(srcLeft, srcTop),
      srcSize = IntSize(srcWidth, srcHeight),
      dstOffset = IntOffset(((side - dstWidth) / 2).toInt(), ((side - dstHeight) / 2).toInt()),
      dstSize = IntSize(dstWidth, dstHeight),
    )
  }
}
