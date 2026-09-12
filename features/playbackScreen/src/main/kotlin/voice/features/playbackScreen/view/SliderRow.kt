package voice.features.playbackScreen.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import voice.core.data.PlaybackTimeDisplay
import voice.core.strings.R
import voice.core.ui.RavenTheme
import voice.core.ui.formatTime
import kotlin.math.roundToInt
import kotlin.time.Duration

@Composable
internal fun SliderRow(
  duration: Duration,
  playedTime: Duration,
  bookDuration: Duration,
  bookPlayedTime: Duration,
  playbackSpeed: Float,
  timeDisplay: PlaybackTimeDisplay,
  onSeek: (Duration) -> Unit,
  onTimeDisplayClick: () -> Unit,
) {
  var dragging by remember { mutableStateOf(false) }
  var localValue by remember { mutableFloatStateOf(0f) }
  var trackWidth by remember { mutableFloatStateOf(1f) }
  val density = LocalDensity.current
  val thumbSize = 16.dp
  val thumbRadiusPx = with(density) { (thumbSize / 2).toPx() }

  val progress = if (dragging) {
    localValue
  } else {
    (playedTime / duration).toFloat().coerceIn(0f, 1f)
  }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(thumbSize)
        .onSizeChanged { trackWidth = it.width.toFloat().coerceAtLeast(1f) }
        .pointerInput(duration) {
          detectTapGestures { offset ->
            onSeek(duration * (offset.x / trackWidth).coerceIn(0f, 1f).toDouble())
          }
        }
        .pointerInput(duration) {
          detectHorizontalDragGestures(
            onDragStart = { offset ->
              dragging = true
              localValue = (offset.x / trackWidth).coerceIn(0f, 1f)
            },
            onDragEnd = {
              onSeek(duration * localValue.toDouble())
              dragging = false
            },
            onDragCancel = { dragging = false },
            onHorizontalDrag = { _, dragAmount ->
              localValue = (localValue + dragAmount / trackWidth).coerceIn(0f, 1f)
            },
          )
        },
      contentAlignment = Alignment.CenterStart,
    ) {
      // track
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(RavenTheme.colors.bgTertiary),
      )
      // fill
      Box(
        modifier = Modifier
          .fillMaxWidth(progress)
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(RavenTheme.colors.primary),
      )
      // thumb
      Box(
        modifier = Modifier
          .offset { IntOffset((progress * trackWidth - thumbRadiusPx).roundToInt(), 0) }
          .size(thumbSize)
          .clip(CircleShape)
          .background(RavenTheme.colors.primary),
      )
    }
    Spacer(Modifier.height(6.dp))
    val shownPlayedTime = if (dragging) duration * localValue.toDouble() else playedTime
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = formatTime(
          timeMs = shownPlayedTime.inWholeMilliseconds,
          durationMs = duration.inWholeMilliseconds,
        ),
        fontSize = 12.sp,
        letterSpacing = (-0.06).sp,
        color = RavenTheme.colors.caption,
      )
      Spacer(Modifier.weight(1f))
      TimeDisplayLabel(
        timeDisplay = timeDisplay,
        chapterDuration = duration,
        chapterPlayedTime = shownPlayedTime,
        bookDuration = bookDuration,
        bookPlayedTime = bookPlayedTime - playedTime + shownPlayedTime,
        playbackSpeed = playbackSpeed,
        onClick = onTimeDisplayClick,
      )
    }
  }
}

@Composable
private fun TimeDisplayLabel(
  timeDisplay: PlaybackTimeDisplay,
  chapterDuration: Duration,
  chapterPlayedTime: Duration,
  bookDuration: Duration,
  bookPlayedTime: Duration,
  playbackSpeed: Float,
  onClick: () -> Unit,
) {
  val speed = playbackSpeed.toDouble().coerceAtLeast(0.01)
  val (time, reference) = when (timeDisplay) {
    PlaybackTimeDisplay.CHAPTER_TOTAL -> chapterDuration to chapterDuration
    PlaybackTimeDisplay.CHAPTER_REMAINING -> (chapterDuration - chapterPlayedTime) to chapterDuration
    PlaybackTimeDisplay.BOOK_REMAINING -> ((bookDuration - bookPlayedTime) / speed) to (bookDuration / speed)
  }
  val prefix = if (timeDisplay == PlaybackTimeDisplay.CHAPTER_TOTAL) "" else "-"
  val caption = when (timeDisplay) {
    PlaybackTimeDisplay.CHAPTER_TOTAL -> stringResource(R.string.time_display_chapter_total)
    PlaybackTimeDisplay.CHAPTER_REMAINING -> stringResource(R.string.time_display_chapter_remaining)
    PlaybackTimeDisplay.BOOK_REMAINING -> stringResource(R.string.time_display_book_remaining)
  }
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier.clickable(onClick = onClick),
  ) {
    Text(
      text = prefix + formatTime(
        timeMs = time.inWholeMilliseconds.coerceAtLeast(0L),
        durationMs = reference.inWholeMilliseconds,
      ),
      fontSize = 12.sp,
      letterSpacing = (-0.06).sp,
      color = RavenTheme.colors.caption,
    )
    Spacer(Modifier.width(4.dp))
    Text(
      text = caption,
      fontSize = 10.sp,
      letterSpacing = (-0.05).sp,
      color = RavenTheme.colors.caption,
    )
  }
}
