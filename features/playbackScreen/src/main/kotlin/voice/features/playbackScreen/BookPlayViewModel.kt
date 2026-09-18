package voice.features.playbackScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.PlaybackTimeDisplay
import voice.core.data.durationMs
import voice.core.data.markForPosition
import voice.core.data.positionInfo
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.sleeptimer.SleepTimerPreference
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.PlaybackTimeDisplayStore
import voice.core.data.store.SleepTimerPreferenceStore
import voice.core.featureflag.ExperimentalPlaybackPersistenceQualifier
import voice.core.featureflag.FeatureFlag
import voice.core.logging.api.Logger
import voice.core.playback.CurrentBookResolver
import voice.core.playback.PlayerController
import voice.core.playback.misc.Decibel
import voice.core.playback.misc.VolumeGain
import voice.core.playback.overlay
import voice.core.playback.playstate.PlaybackHistoryRecorder
import voice.core.playback.playstate.PlayStateManager
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerMode.TimedWithDuration
import voice.core.sleeptimer.SleepTimerState
import voice.core.ui.ImmutableFile
import voice.core.ui.formatDuration
import voice.core.ui.formatTime
import voice.features.playbackScreen.batteryOptimization.BatteryOptimization
import voice.features.sleepTimer.SleepTimerViewState
import voice.features.playbackScreen.view.BookmarkChapterGroup
import voice.features.playbackScreen.view.BookmarkItemUi
import voice.features.playbackScreen.view.EditBookmarkState
import voice.navigation.Destination
import voice.navigation.Navigator
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@AssistedInject
class BookPlayViewModel(
  private val bookRepository: BookRepository,
  private val currentBookResolver: CurrentBookResolver,
  private val player: PlayerController,
  private val sleepTimer: SleepTimer,
  private val playStateManager: PlayStateManager,
  @CurrentBookStore
  private val currentBookStoreId: DataStore<BookId?>,
  private val navigator: Navigator,
  private val bookmarkRepository: BookmarkRepo,
  private val historyRecorder: PlaybackHistoryRecorder,
  private val volumeGainFormatter: VolumeGainFormatter,
  private val batteryOptimization: BatteryOptimization,
  dispatcherProvider: DispatcherProvider,
  @SleepTimerPreferenceStore
  private val sleepTimerPreferenceStore: DataStore<SleepTimerPreference>,
  @PlaybackTimeDisplayStore
  private val playbackTimeDisplayStore: DataStore<PlaybackTimeDisplay>,
  @ExperimentalPlaybackPersistenceQualifier
  private val experimentalPlaybackPersistenceFeatureFlag: FeatureFlag<Boolean>,
  @Assisted
  private val bookId: BookId,
) {

  private val scope = MainScope(dispatcherProvider)

  private val _viewEffects = MutableSharedFlow<BookPlayViewEffect>(extraBufferCapacity = 1)
  internal val viewEffects: Flow<BookPlayViewEffect> get() = _viewEffects

  private val _dialogState = mutableStateOf<BookPlayDialogViewState?>(null)
  internal val dialogState: State<BookPlayDialogViewState?> get() = _dialogState

  init {
    scope.launch {
      player.pauseIfCurrentBookDifferentFrom(bookId)
      currentBookStoreId.updateData { bookId }
      // Warm the player while the screen renders so pressing play resumes instantly.
      player.prepare()
    }
  }

  @Composable
  fun viewState(): BookPlayViewState? {
    val persistedBook = remember(bookId) {
      bookRepository.flow(bookId).filterNotNull()
    }.collectAsState(initial = null).value ?: return null

    val experimentalPlaybackPersistence = experimentalPlaybackPersistenceFeatureFlag.get()
    val livePlaybackState = if (experimentalPlaybackPersistence) {
      remember(bookId) { player.livePlaybackStateFlow(bookId) }
        .collectAsState(null).value
    } else {
      null
    }
    val managerPlayState by remember {
      playStateManager.flow
    }.collectAsState()

    val book = if (livePlaybackState != null) {
      persistedBook.overlay(livePlaybackState)
    } else {
      persistedBook
    }
    val isPlaying = livePlaybackState?.isPlaying ?: (managerPlayState == PlayStateManager.PlayState.Playing)

    val currentMark = book.currentChapter.markForPosition(book.content.positionInChapter)
    val positionInCurrentMark = if (isPlaying && currentMark.durationMs > 0) {
      val relativePosition = book.content.positionInChapter - currentMark.startMs
      relativePosition.coerceIn(0L, currentMark.durationMs)
    } else {
      book.content.positionInChapter - currentMark.startMs
    }

    val sleepTime = remember { sleepTimer.state }.collectAsState().value
    val timeDisplay = remember { playbackTimeDisplayStore.data }
      .collectAsState(initial = PlaybackTimeDisplay.CHAPTER_TOTAL).value
    val hasMoreThanOneChapter = book.chapters.sumOf { it.chapterMarks.count() } > 1
    return BookPlayViewState(
      sleepTimerState = sleepTime.toViewState(),
      playing = isPlaying,
      title = book.content.name,
      author = book.content.author,
      playbackSpeed = book.content.playbackSpeed,
      showPreviousNextButtons = hasMoreThanOneChapter,
      chapterName = currentMark.name.takeIf { hasMoreThanOneChapter },
      duration = currentMark.durationMs.milliseconds,
      playedTime = positionInCurrentMark.milliseconds,
      bookPlayedTime = book.position.milliseconds,
      bookDuration = book.duration.milliseconds,
      timeDisplay = timeDisplay,
      cover = book.content.cover?.let(::ImmutableFile),
      accentColor = book.content.accentColor,
      skipSilence = book.content.skipSilence,
    )
  }

  fun dismissDialog() {
    Logger.d("dismissDialog")
    _dialogState.value = null
  }

  fun incrementSleepTime() {
    updateSleepTimeViewState {
      val customTime = it.customSleepTime
      val newTime = customTime + 1
      sleepTimerPreferenceStore.updateData { preference -> preference.copy(duration = newTime.minutes) }
      SleepTimerViewState(newTime)
    }
  }

  fun decrementSleepTime() {
    updateSleepTimeViewState {
      val customTime = it.customSleepTime
      val newTime = (customTime - 1).coerceAtLeast(1)
      sleepTimerPreferenceStore.updateData { preference ->
        preference.copy(duration = newTime.minutes)
      }
      SleepTimerViewState(newTime)
    }
  }

  fun onAcceptSleepTime(time: Int) {
    updateSleepTimeViewState {
      val book = currentBook() ?: return@updateSleepTimeViewState null
      scope.launch {
        sleepTimerPreferenceStore.updateData { preference ->
          preference.copy(
            duration = time.minutes,
            persistentSleepTimerEnabled = true,
          )
        }
        bookmarkRepository.addBookmarkAtBookPosition(
          book = book,
          setBySleepTimer = true,
          title = null,
        )
      }
      sleepTimer.enable(TimedWithDuration(time.minutes))
      null
    }
  }

  fun onAcceptSleepAtEndOfChapter() {
    updateSleepTimeViewState {
      sleepTimer.enable(SleepTimerMode.EndOfChapter)
      null
    }
  }

  private fun updateSleepTimeViewState(update: suspend (SleepTimerViewState) -> SleepTimerViewState?) {
    scope.launch {
      val current = dialogState.value
      val updated: SleepTimerViewState? = if (current is BookPlayDialogViewState.SleepTimer) {
        update(current.viewState)
      } else {
        update(SleepTimerViewState(sleepTimerPreferenceStore.data.first().duration.inWholeMinutes.toInt()))
      }
      _dialogState.value = updated?.let(BookPlayDialogViewState::SleepTimer)
    }
  }

  fun onPlaybackSpeedChanged(speed: Float) {
    _dialogState.value = BookPlayDialogViewState.SpeedDialog(speed)
    player.setSpeed(speed)
  }

  fun onVolumeGainChanged(gain: Decibel) {
    _dialogState.value = volumeGainDialogViewState(gain)
    player.setGain(gain)
  }

  fun next() {
    player.next()
  }

  fun previous() {
    player.previous()
  }

  fun playPause() {
    val isPlaying = playStateManager.playState == PlayStateManager.PlayState.Playing
    if (!isPlaying) {
      scope.launch {
        if (batteryOptimization.shouldRequest()) {
          _viewEffects.tryEmit(BookPlayViewEffect.RequestIgnoreBatteryOptimization)
          batteryOptimization.onBatteryOptimizationsRequested()
        }
      }
    }
    player.playPause()
  }

  fun rewind() {
    player.rewind()
  }

  fun fastForward() {
    player.fastForward()
  }

  fun onCloseClick() {
    navigator.goBack()
  }

  fun onBookDetailsClick() {
    navigator.goTo(Destination.BookDetails(bookId))
  }

  fun onCurrentChapterClick() {
    scope.launch {
      val book = currentBook() ?: return@launch
      _dialogState.value = BookPlayDialogViewState.SelectChapterDialog(
        items = book.chapters.flatMapIndexed { chapterIndex, chapter ->
          chapter.chapterMarks.mapIndexed { markIndex, chapterMark ->
            val previousChapters = book.chapters.take(chapterIndex)
            BookPlayDialogViewState.SelectChapterDialog.ItemViewState(
              number = previousChapters.sumOf { it.chapterMarks.count() } + markIndex + 1,
              name = chapterMark.name ?: "",
              active = chapterMark == book.currentMark && chapter == book.currentChapter,
              time = formatTime(previousChapters.sumOf { it.duration } + chapterMark.startMs),
              duration = formatDuration(chapterMark.durationMs),
            )
          }
        },
      )
    }
  }

  fun onChapterClick(number: Int) {
    scope.launch {
      val book = currentBook() ?: return@launch
      var currentIndex = -1
      book.chapters.forEach { chapter ->
        chapter.chapterMarks.forEach { mark ->
          currentIndex++
          if (currentIndex == number - 1) {
            historyRecorder.onSkippedToChapter()
            player.setPosition(mark.startMs, chapter.id)
            _dialogState.value = null
            return@launch
          }
        }
      }
    }
  }

  fun onPlaybackSpeedIconClick() {
    scope.launch {
      val playbackSpeed = currentBook()?.content?.playbackSpeed ?: return@launch
      _dialogState.value = BookPlayDialogViewState.SpeedDialog(playbackSpeed)
    }
  }

  fun onVolumeGainIconClick() {
    scope.launch {
      val content = currentBook()?.content ?: return@launch
      _dialogState.value = volumeGainDialogViewState(Decibel(content.gain))
    }
  }

  private fun volumeGainDialogViewState(gain: Decibel): BookPlayDialogViewState.VolumeGainDialog {
    return BookPlayDialogViewState.VolumeGainDialog(
      gain = gain,
      maxGain = VolumeGain.MAX_GAIN,
      valueFormatted = volumeGainFormatter.format(gain),
    )
  }

  private val _showBookmarksSheet = mutableStateOf(false)
  val showBookmarksSheet: State<Boolean> get() = _showBookmarksSheet
  private val _bookmarkGroups = mutableStateOf<List<BookmarkChapterGroup>>(emptyList())
  val bookmarkGroups: State<List<BookmarkChapterGroup>> get() = _bookmarkGroups
  private var _allBookmarks: List<Bookmark> = emptyList()

  private val _showAddBookmarkDialog = mutableStateOf(false)
  val showAddBookmarkDialog: State<Boolean> get() = _showAddBookmarkDialog

  private val _editBookmarkState = mutableStateOf<EditBookmarkState?>(null)
  val editBookmarkState: State<EditBookmarkState?> get() = _editBookmarkState

  fun onBookmarkClick() {
    scope.launch {
      val book = currentBook() ?: return@launch
      _allBookmarks = bookmarkRepository.bookmarks(book.content)
      _bookmarkGroups.value = buildBookmarkGroups(book, _allBookmarks)
      _showBookmarksSheet.value = true
    }
  }

  fun dismissBookmarksSheet() {
    _showBookmarksSheet.value = false
  }

  fun onAddBookmarkFromSheet() {
    _showBookmarksSheet.value = false
    _showAddBookmarkDialog.value = true
  }

  fun dismissAddBookmarkDialog() {
    _showAddBookmarkDialog.value = false
  }

  fun saveNewBookmark(title: String) {
    scope.launch {
      val book = currentBook() ?: return@launch
      bookmarkRepository.addBookmarkAtBookPosition(
        book = book,
        title = title.ifBlank { null },
        setBySleepTimer = false,
      )
      _viewEffects.tryEmit(BookPlayViewEffect.BookmarkAdded)
    }
  }

  fun onEditBookmark(bookmarkId: Bookmark.Id) {
    val bookmark = _allBookmarks.find { it.id == bookmarkId } ?: return
    scope.launch {
      val book = currentBook() ?: return@launch
      val chapter = book.chapters.find { it.id == bookmark.chapterId }
      val info = chapter?.positionInfo(bookmark.time)
      _showBookmarksSheet.value = false
      _editBookmarkState.value = EditBookmarkState(
        bookmarkId = bookmarkId,
        chapterInfo = "${info?.name ?: ""} - ${voice.core.ui.formatTime(info?.positionInMarkMs ?: 0L, info?.markDurationMs ?: 0L)}",
        currentTitle = bookmark.title ?: "",
      )
    }
  }

  fun dismissEditBookmark() {
    _editBookmarkState.value = null
  }

  fun saveEditedBookmark(bookmarkId: Bookmark.Id, newTitle: String) {
    val bookmark = _allBookmarks.find { it.id == bookmarkId } ?: return
    scope.launch {
      bookmarkRepository.addBookmark(bookmark.copy(title = newTitle.ifBlank { null }))
    }
  }

  fun deleteBookmark(bookmarkId: Bookmark.Id) {
    scope.launch {
      bookmarkRepository.deleteBookmark(bookmarkId)
      _allBookmarks = _allBookmarks.filter { it.id != bookmarkId }
      val book = currentBook() ?: return@launch
      _bookmarkGroups.value = buildBookmarkGroups(book, _allBookmarks)
    }
  }

  fun onBookmarkItemClick(bookmarkId: Bookmark.Id) {
    val bookmark = _allBookmarks.find { it.id == bookmarkId } ?: return
    player.setPosition(bookmark.time, bookmark.chapterId)
    _showBookmarksSheet.value = false
  }

  fun exportBookmarks() {
    scope.launch {
      val book = currentBook() ?: return@launch
      val text = buildString {
        appendLine("${book.content.name} Bookmarks")
        appendLine()
        _allBookmarks
          .groupBy { it.chapterId }
          .forEach { (chapterId, bookmarks) ->
            val chapter = book.chapters.find { it.id == chapterId }
            appendLine(chapter?.name ?: "Unknown Chapter")
            bookmarks.forEach { bm ->
              val time = voice.core.ui.formatTime(bm.time, chapter?.duration ?: 0L)
              appendLine("  [$time] ${bm.title ?: "Bookmark"}")
            }
            appendLine()
          }
      }
      _viewEffects.tryEmit(BookPlayViewEffect.ExportBookmarks(text))
    }
  }

  private fun buildBookmarkGroups(book: Book, bookmarks: List<Bookmark>): List<BookmarkChapterGroup> {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT)
    val zone = ZoneId.systemDefault()
    val chaptersById = book.chapters.associateBy { it.id }
    return bookmarks
      .filter { !it.setBySleepTimer }
      .mapNotNull { bm ->
        val chapter = chaptersById[bm.chapterId] ?: return@mapNotNull null
        Triple(bm, chapter.positionInfo(bm.time), bm)
      }
      .groupBy { it.second.name ?: "Chapter" }
      .map { (chapterName, entries) ->
        BookmarkChapterGroup(
          chapterName = chapterName,
          items = entries.sortedByDescending { it.first.addedAt }.map { (bm, info, _) ->
            BookmarkItemUi(
              id = bm.id,
              title = bm.title ?: "",
              timeAndDate = "${voice.core.ui.formatTime(info.positionInMarkMs, info.markDurationMs)} - ${dateFormatter.format(bm.addedAt.atZone(zone).toLocalDate())}",
            )
          },
        )
      }
  }

  private val _showHistorySheet = mutableStateOf(false)
  val showHistorySheet: State<Boolean> get() = _showHistorySheet

  fun onHistoryClick() {
    _showHistorySheet.value = true
  }

  fun dismissHistorySheet() {
    _showHistorySheet.value = false
  }

  fun onBookmarkLongClick() {
    scope.launch {
      val book = currentBook() ?: return@launch
      bookmarkRepository.addBookmarkAtBookPosition(
        book = book,
        title = null,
        setBySleepTimer = false,
      )
      _viewEffects.tryEmit(BookPlayViewEffect.BookmarkAdded)
    }
  }

  fun cycleTimeDisplay() {
    scope.launch {
      playbackTimeDisplayStore.updateData { it.next() }
    }
  }

  fun seekTo(position: Duration) {
    scope.launch {
      val book = currentBook() ?: return@launch
      val currentChapter = book.currentChapter
      val currentMark = currentChapter.markForPosition(book.content.positionInChapter)
      historyRecorder.onJump()
      player.setPosition(currentMark.startMs + position.inWholeMilliseconds, currentChapter.id)
    }
  }

  fun toggleSleepTimer() {
    scope.launch {
      Logger.d("toggleSleepTimer while active=${sleepTimer.state.value}")
      if (sleepTimer.state.value.enabled) {
        sleepTimer.disable()
        sleepTimerPreferenceStore.updateData { it.copy(persistentSleepTimerEnabled = false) }
        _dialogState.value = null
      } else {
        _dialogState.value = BookPlayDialogViewState.SleepTimer(
          viewState = SleepTimerViewState(
            customSleepTime = sleepTimerPreferenceStore.data.first().duration.inWholeMinutes.toInt(),
          ),
        )
      }
    }
  }

  fun onBatteryOptimizationRequested() {
    navigator.goTo(Destination.BatteryOptimization)
  }

  fun toggleSkipSilence() {
    scope.launch {
      val skipSilence = currentBook()?.content?.skipSilence ?: return@launch
      player.skipSilence(!skipSilence)
    }
  }

  private suspend fun currentBook(): Book? {
    return currentBookResolver.book(bookId)
  }

  @AssistedFactory
  interface Factory {
    fun create(bookId: BookId): BookPlayViewModel
  }
}

private fun SleepTimerState.toViewState(): BookPlayViewState.SleepTimerViewState = when (this) {
  SleepTimerState.Disabled -> BookPlayViewState.SleepTimerViewState.Disabled
  is SleepTimerState.Enabled.WithDuration -> BookPlayViewState.SleepTimerViewState.Enabled.WithDuration(this.leftDuration)
  SleepTimerState.Enabled.WithEndOfChapter -> BookPlayViewState.SleepTimerViewState.Enabled.WithEndOfChapter
}
