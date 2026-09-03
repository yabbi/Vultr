package voice.features.playbackScreen.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.BookId
import voice.core.data.ListeningHistoryAction
import voice.core.data.ListeningSession
import voice.core.data.positionInfo
import voice.core.data.repo.BookRepository
import voice.core.data.repo.ListeningSessionRepo
import voice.core.ui.formatTime
import voice.navigation.Navigator
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

@AssistedInject
class HistoryViewModel(
  private val listeningSessionRepo: ListeningSessionRepo,
  private val bookRepository: BookRepository,
  private val navigator: Navigator,
  private val playerController: voice.core.playback.PlayerController,
  dispatcherProvider: DispatcherProvider,
  @Assisted
  private val bookId: BookId,
) {

  private val scope = MainScope(dispatcherProvider)
  private val _state = mutableStateOf(HistoryViewState(title = "", days = emptyList()))
  val state: State<HistoryViewState> get() = _state

  init {
    refresh()
  }

  private fun refresh() {
    scope.launch {
      val book = bookRepository.get(bookId)
      val chapters = book?.chapters.orEmpty()
      val chaptersById = chapters.associateBy { it.id }
      val bookDurationMs = chapters.sumOf { it.duration }
      val zone = ZoneId.systemDefault()
      val days = listeningSessionRepo.forBook(bookId)
        .sortedByDescending { it.createdAt }
        .groupBy { it.createdAt.atZone(zone).toLocalDate() }
        .map { (date, sessions) ->
          val totalListenedMs = sessions.sumOf { it.listenedMs }
          val times = sessions.map { it.createdAt.atZone(zone).toLocalTime() }
          HistoryDayViewState(
            date = date,
            firstTime = times.min(),
            lastTime = times.max(),
            totalListenedMs = totalListenedMs,
            entries = sessions.map { session ->
              val info = chaptersById[session.chapterId]?.positionInfo(session.positionInChapter)
              val positionText = if (info != null) {
                "${formatTime(info.positionInMarkMs, info.markDurationMs)}/${formatTime(info.markDurationMs, info.markDurationMs)}"
              } else {
                formatTime(session.positionInChapter)
              }
              // Global book position: durations of all earlier chapters + position within this chapter.
              // Mirrors Book.position so it matches the player.
              val globalPositionMs = chapters
                .takeWhile { it.id != session.chapterId }
                .sumOf { it.duration } + session.positionInChapter
              HistoryEntryViewState(
                id = session.id,
                action = runCatching { ListeningHistoryAction.valueOf(session.action) }.getOrNull(),
                time = session.createdAt.atZone(zone).toLocalTime(),
                listenedMs = session.listenedMs,
                chapterName = info?.name,
                positionText = positionText,
                globalPositionText = formatTime(globalPositionMs, bookDurationMs),
              )
            },
          )
        }
      _state.value = HistoryViewState(
        title = book?.content?.name.orEmpty(),
        days = days,
      )
    }
  }

  @Composable
  fun viewState(): HistoryViewState = state.value

  fun onCloseClick() {
    navigator.goBack()
  }

  fun onDelete(id: ListeningSession.Id) {
    scope.launch {
      listeningSessionRepo.delete(id)
      refresh()
    }
  }

  fun onEntryClick(id: ListeningSession.Id) {
    scope.launch {
      val session = listeningSessionRepo.forBook(bookId).find { it.id == id } ?: return@launch
      playerController.setPosition(session.positionInChapter, session.chapterId)
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(bookId: BookId): HistoryViewModel
  }
}

data class HistoryViewState(
  val title: String,
  val days: List<HistoryDayViewState>,
)

data class HistoryDayViewState(
  val date: LocalDate,
  val firstTime: LocalTime,
  val lastTime: LocalTime,
  val totalListenedMs: Long,
  val entries: List<HistoryEntryViewState>,
)

data class HistoryEntryViewState(
  val id: ListeningSession.Id,
  val action: ListeningHistoryAction?,
  val time: LocalTime,
  val listenedMs: Long,
  val chapterName: String?,
  val positionText: String,
  val globalPositionText: String,
)
