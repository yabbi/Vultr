package voice.features.bookOverview.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import voice.core.ui.ImmutableFile
import voice.core.ui.formatTime
import voice.features.bookOverview.overview.MiniPlayerViewState
import voice.features.bookOverview.overview.toMiniPlayerViewState
import voice.navigation.Destination
import voice.navigation.Navigator

@AssistedInject
class BookDetailsViewModel(
  private val repo: BookRepository,
  private val navigator: Navigator,
  private val player: PlayerController,
  private val playStateManager: PlayStateManager,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  @Assisted
  private val bookId: BookId,
) {

  private val scope = MainScope()

  init {
    scope.launch {
      // If this is the book in progress, warm the player on screen open so that
      // pressing play to continue resumes instantly instead of preparing on tap.
      if (currentBookStore.data.first() == bookId) {
        player.prepare()
      }
    }
  }

  @Composable
  fun viewState(): BookDetailsViewState? {
    val book = remember(bookId) { repo.flow(bookId).filterNotNull() }
      .collectAsState(initial = null).value ?: return null
    val currentBookId = remember { currentBookStore.data }.collectAsState(initial = null).value
    val playState = remember { playStateManager.flow }
      .collectAsState(initial = PlayStateManager.PlayState.Paused).value
    val isPlaying = currentBookId == bookId && playState == PlayStateManager.PlayState.Playing
    return book.toDetailsViewState(
      isPlaying = isPlaying,
      miniPlayer = currentBookId?.let { currentId ->
        repo.flow(currentId).collectAsState(initial = null).value
          ?.toMiniPlayerViewState(playState == PlayStateManager.PlayState.Playing)
      },
    )
  }

  fun onBackClick() {
    navigator.goBack()
  }

  fun onPlayClick() {
    scope.launch {
      if (currentBookStore.data.first() == bookId) {
        // Already the active book: toggle in place so the cover button pauses/resumes
        // and its icon stays in sync with the actual play state.
        player.playPause()
      } else {
        currentBookStore.updateData { bookId }
        navigator.goTo(Destination.Playback(bookId))
        player.play()
      }
    }
  }

  fun onChapterClick(chapter: BookDetailsViewState.ChapterViewState) {
    scope.launch {
      currentBookStore.updateData { bookId }
      player.setPosition(chapter.startMs, chapter.chapterId)
      navigator.goTo(Destination.Playback(bookId))
      player.play()
    }
  }

  fun onMiniPlayerClick(id: BookId) {
    navigator.goTo(Destination.Playback(id))
  }

  fun onMiniPlayerPlayClick() {
    player.playPause()
  }

  fun onEditClick() {
    navigator.goTo(Destination.EditBook(bookId))
  }

  @AssistedFactory
  interface Factory {
    fun create(bookId: BookId): BookDetailsViewModel
  }
}

data class EditBookForm(
  val title: String,
  val author: String,
  val date: String,
  val description: String,
  val cover: ImmutableFile?,
  val accentColor: Int?,
)

private fun formatHoursMinutes(ms: Long): String {
  val totalMinutes = ms.coerceAtLeast(0) / 60_000
  val hours = totalMinutes / 60
  val minutes = totalMinutes % 60
  return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
}

data class BookDetailsViewState(
  val title: String,
  val author: String?,
  val cover: ImmutableFile?,
  val accentColor: Int?,
  val progress: Float,
  val remainingTime: String,
  val durationText: String,
  val chapterCount: Int,
  val year: Int?,
  val description: String,
  val chapters: List<ChapterViewState>,
  val isPlaying: Boolean,
  val miniPlayer: MiniPlayerViewState?,
) {
  data class ChapterViewState(
    val number: Int,
    val chapterId: ChapterId,
    val title: String,
    val time: String,
    val startMs: Long,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
  )
}

private fun Book.toDetailsViewState(
  isPlaying: Boolean,
  miniPlayer: MiniPlayerViewState?,
) = BookDetailsViewState(
  title = content.name,
  author = content.author,
  cover = content.cover?.let(::ImmutableFile),
  accentColor = content.accentColor,
  progress = (position.toFloat() / duration.toFloat()).coerceIn(0F, 1F),
  remainingTime = formatHoursMinutes(duration - position) + " left",
  durationText = formatHoursMinutes(duration),
  chapterCount = chapters.sumOf { it.chapterMarks.size },
  year = content.year,
  description = content.description.orEmpty(),
  chapters = run {
    val currentChapterIndex = content.currentChapterIndex
    val currentMarkStartMs = currentMark.startMs
    chapters.flatMapIndexed { chapterIndex, chapter ->
      chapter.chapterMarks.map { mark ->
        BookDetailsViewState.ChapterViewState(
          number = 0,
          chapterId = chapter.id,
          title = mark.name ?: chapter.name ?: content.name,
          time = formatTime(mark.startMs),
          startMs = mark.startMs,
          isCompleted = chapterIndex < currentChapterIndex ||
            (chapterIndex == currentChapterIndex && mark.startMs < currentMarkStartMs),
          isCurrent = chapterIndex == currentChapterIndex && mark.startMs == currentMarkStartMs,
        )
      }
    }.mapIndexed { index, chapter -> chapter.copy(number = index + 1) }
  },
  isPlaying = isPlaying,
  miniPlayer = miniPlayer,
)
