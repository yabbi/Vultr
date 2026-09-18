package voice.features.bookOverview.overview

import androidx.compose.runtime.Immutable
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.logging.api.Logger
import voice.core.ui.ImmutableFile
import voice.core.ui.formatTime
import java.time.Instant

@Immutable
data class BookOverviewItemViewState(
  val name: String,
  val author: String?,
  val cover: ImmutableFile?,
  val progress: Float,
  val id: BookId,
  val remainingTime: String,
  val addedAt: Instant,
)

@Immutable
data class MiniPlayerViewState(
  val id: BookId,
  val chapterTitle: String,
  val author: String?,
  val cover: ImmutableFile?,
  val accentColor: Int?,
  val progress: Float,
  val positionText: String,
  val durationText: String,
  val playing: Boolean,
)

internal fun Book.toItemViewState() = BookOverviewItemViewState(
  name = content.name,
  author = content.author,
  cover = content.cover?.let(::ImmutableFile),
  id = id,
  progress = progress(),
  remainingTime = formatTime(duration - position),
  addedAt = content.addedAt,
)

internal fun Book.toMiniPlayerViewState(playing: Boolean) = MiniPlayerViewState(
  id = id,
  chapterTitle = currentMark.name ?: currentChapter.name ?: content.name,
  author = content.author,
  cover = content.cover?.let(::ImmutableFile),
  accentColor = content.accentColor,
  progress = progress(),
  // Global book position / total book duration, matching the player and notification.
  positionText = formatTime(position, duration),
  durationText = formatTime(duration),
  playing = playing,
)

private fun Book.progress(): Float {
  val globalPosition = position
  val totalDuration = duration
  val progress = globalPosition.toFloat() / totalDuration.toFloat()
  if (progress < 0F) {
    Logger.w("Couldn't determine progress for book=$this")
  }
  return progress.coerceIn(0F, 1F)
}
