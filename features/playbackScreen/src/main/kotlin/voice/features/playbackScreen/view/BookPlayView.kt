package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import voice.core.data.PlaybackTimeDisplay
import voice.core.ui.VoiceTheme
import voice.features.playbackScreen.BookPlayViewState
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun BookPlayView(
  viewState: BookPlayViewState,
  useLandscapeLayout: Boolean,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
  onSeek: (Duration) -> Unit,
  onTimeDisplayClick: () -> Unit,
  onSleepTimerClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onBookmarkLongClick: () -> Unit,
  onAddBookmarkClick: () -> Unit,
  onHistoryClick: () -> Unit,
  onSpeedChangeClick: () -> Unit,
  onSkipSilenceClick: () -> Unit,
  onVolumeBoostClick: () -> Unit,
  onSkipToNext: () -> Unit,
  onSkipToPrevious: () -> Unit,
  onCloseClick: () -> Unit,
  onCurrentChapterClick: () -> Unit,
  onBookDetailsClick: () -> Unit,
  snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
  val speedText = formatSpeed(viewState.playbackSpeed)
  Scaffold(
    snackbarHost = {
      SnackbarHost(hostState = snackbarHostState)
    },
    topBar = {
      BookPlayAppBar(
        viewState = viewState,
        onAddBookmarkClick = onAddBookmarkClick,
        onSkipSilenceClick = onSkipSilenceClick,
        onVolumeBoostClick = onVolumeBoostClick,
        onCloseClick = onCloseClick,
      )
    },
    bottomBar = {
      // Portrait: actions docked to the system nav bar.
      // Landscape: the same bar sits in the right control column (see BookPlayContent).
      if (!useLandscapeLayout) {
        PlayerActionsBar(
          speedText = speedText,
          speedActive = viewState.playbackSpeed != 1.0f,
          sleepTimerState = viewState.sleepTimerState,
          onSpeedClick = onSpeedChangeClick,
          onBookmarksClick = onBookmarkClick,
          onHistoryClick = onHistoryClick,
          onSleepClick = onSleepTimerClick,
          modifier = Modifier.navigationBarsPadding(),
        )
      }
    },
    content = {
      BookPlayContent(
        contentPadding = it,
        viewState = viewState,
        onPlayClick = onPlayClick,
        onRewindClick = onRewindClick,
        onFastForwardClick = onFastForwardClick,
        onSeek = onSeek,
        onTimeDisplayClick = onTimeDisplayClick,
        onSkipToNext = onSkipToNext,
        onSkipToPrevious = onSkipToPrevious,
        onCurrentChapterClick = onCurrentChapterClick,
        onBookDetailsClick = onBookDetailsClick,
        onSpeedChangeClick = onSpeedChangeClick,
        onBookmarkClick = onBookmarkClick,
        onHistoryClick = onHistoryClick,
        onSleepTimerClick = onSleepTimerClick,
        useLandscapeLayout = useLandscapeLayout,
        speedText = speedText,
      )
    },
  )
}

@Composable
@Preview
private fun BookPlayPreview(
  @PreviewParameter(BookPlayViewStatePreviewProvider::class)
  viewState: BookPlayViewState,
) {
  VoiceTheme {
    BookPlayView(
      viewState = viewState,
      onPlayClick = {},
      onRewindClick = {},
      onFastForwardClick = {},
      onSeek = {},
      onTimeDisplayClick = {},
      onSleepTimerClick = {},
      onBookmarkClick = {},
      onBookmarkLongClick = {},
      onAddBookmarkClick = {},
      onHistoryClick = {},
      onSpeedChangeClick = {},
      onSkipSilenceClick = {},
      onVolumeBoostClick = {},
      onSkipToNext = {},
      onSkipToPrevious = {},
      onCloseClick = {},
      onCurrentChapterClick = {},
      onBookDetailsClick = {},
      useLandscapeLayout = false,
    )
  }
}

private class BookPlayViewStatePreviewProvider : PreviewParameterProvider<BookPlayViewState> {
  override val values = sequence {
    val initial = BookPlayViewState(
      chapterName = "My Chapter",
      showPreviousNextButtons = false,
      cover = null,
      accentColor = null,
      duration = 10.minutes,
      playedTime = 3.minutes,
      bookDuration = 120.minutes,
      bookPlayedTime = 43.minutes,
      timeDisplay = PlaybackTimeDisplay.CHAPTER_TOTAL,
      playing = true,
      skipSilence = true,
      sleepTimerState = BookPlayViewState.SleepTimerViewState.Disabled,
      title = "Das Ende der Welt",
      author = "Max Mustermann",
      playbackSpeed = 1.2f,
    )
    yield(initial)
    yield(
      initial.copy(
        showPreviousNextButtons = !initial.showPreviousNextButtons,
        playing = !initial.playing,
        skipSilence = !initial.skipSilence,
      ),
    )
    yield(initial.copy(chapterName = null))
  }
}
