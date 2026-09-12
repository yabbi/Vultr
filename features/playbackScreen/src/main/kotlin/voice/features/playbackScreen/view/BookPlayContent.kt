package voice.features.playbackScreen.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import voice.core.ui.RavenTheme
import voice.features.playbackScreen.BookPlayViewState
import kotlin.time.Duration

@Composable
internal fun BookPlayContent(
  contentPadding: PaddingValues,
  viewState: BookPlayViewState,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
  onSeek: (Duration) -> Unit,
  onTimeDisplayClick: () -> Unit,
  onSkipToNext: () -> Unit,
  onSkipToPrevious: () -> Unit,
  onCurrentChapterClick: () -> Unit,
  onBookDetailsClick: () -> Unit,
  onSpeedChangeClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onHistoryClick: () -> Unit,
  onSleepTimerClick: () -> Unit,
  useLandscapeLayout: Boolean,
  speedText: String,
) {
  if (useLandscapeLayout) {
    LandscapeContent(
      contentPadding = contentPadding,
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
      speedText = speedText,
    )
  } else {
    PortraitContent(
      contentPadding = contentPadding,
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
    )
  }
}

@Composable
private fun PortraitContent(
  contentPadding: PaddingValues,
  viewState: BookPlayViewState,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
  onSeek: (Duration) -> Unit,
  onTimeDisplayClick: () -> Unit,
  onSkipToNext: () -> Unit,
  onSkipToPrevious: () -> Unit,
  onCurrentChapterClick: () -> Unit,
  onBookDetailsClick: () -> Unit,
) {
  Column(Modifier.padding(contentPadding)) {
    Spacer(modifier = Modifier.size(8.dp))
    PlayerHeader(
      title = viewState.title,
      author = viewState.author,
      onClick = onBookDetailsClick,
    )
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1F)
        .padding(horizontal = 16.dp, vertical = 16.dp),
      contentAlignment = Alignment.Center,
    ) {
      CoverRow(
        onPlayClick = onPlayClick,
        cover = viewState.cover,
        modifier = Modifier
          .fillMaxWidth(0.82f)
          .aspectRatio(1F),
      )
    }
    viewState.chapterName?.let { chapterName ->
      ChapterRow(chapterName = chapterName, onClick = onCurrentChapterClick)
    }
    Spacer(modifier = Modifier.size(16.dp))
    SliderRow(
      duration = viewState.duration,
      playedTime = viewState.playedTime,
      bookDuration = viewState.bookDuration,
      bookPlayedTime = viewState.bookPlayedTime,
      playbackSpeed = viewState.playbackSpeed,
      timeDisplay = viewState.timeDisplay,
      onSeek = onSeek,
      onTimeDisplayClick = onTimeDisplayClick,
    )
    Spacer(modifier = Modifier.size(16.dp))
    PlaybackRow(
      playing = viewState.playing,
      showPreviousNext = viewState.showPreviousNextButtons,
      onPlayClick = onPlayClick,
      onRewindClick = onRewindClick,
      onFastForwardClick = onFastForwardClick,
      onSkipToPrevious = onSkipToPrevious,
      onSkipToNext = onSkipToNext,
    )
    Spacer(modifier = Modifier.size(24.dp))
  }
}

@Composable
private fun LandscapeContent(
  contentPadding: PaddingValues,
  viewState: BookPlayViewState,
  onPlayClick: () -> Unit,
  onRewindClick: () -> Unit,
  onFastForwardClick: () -> Unit,
  onSeek: (Duration) -> Unit,
  onTimeDisplayClick: () -> Unit,
  onSkipToNext: () -> Unit,
  onSkipToPrevious: () -> Unit,
  onCurrentChapterClick: () -> Unit,
  onBookDetailsClick: () -> Unit,
  onSpeedChangeClick: () -> Unit,
  onBookmarkClick: () -> Unit,
  onHistoryClick: () -> Unit,
  onSleepTimerClick: () -> Unit,
  speedText: String,
) {
  Row(
    modifier = Modifier
      .padding(contentPadding)
      .fillMaxWidth()
      .fillMaxHeight(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Box(
      modifier = Modifier
        .weight(1F)
        .fillMaxHeight()
        .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
      contentAlignment = Alignment.Center,
    ) {
      CoverRow(
        cover = viewState.cover,
        onPlayClick = onPlayClick,
        modifier = Modifier
          .fillMaxHeight()
          .aspectRatio(1F),
      )
    }
    Column(
      modifier = Modifier
        .weight(1F)
        .fillMaxHeight()
        .padding(end = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Spacer(modifier = Modifier.weight(0.4f))
      PlayerHeader(
        title = viewState.title,
        author = viewState.author,
        onClick = onBookDetailsClick,
      )
      Spacer(modifier = Modifier.weight(1f))
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
      ) {
        viewState.chapterName?.let { chapterName ->
          ChapterRow(chapterName = chapterName, onClick = onCurrentChapterClick)
        }
        Spacer(modifier = Modifier.size(12.dp))
        SliderRow(
          duration = viewState.duration,
          playedTime = viewState.playedTime,
          bookDuration = viewState.bookDuration,
          bookPlayedTime = viewState.bookPlayedTime,
          playbackSpeed = viewState.playbackSpeed,
          timeDisplay = viewState.timeDisplay,
          onSeek = onSeek,
          onTimeDisplayClick = onTimeDisplayClick,
        )
        Spacer(modifier = Modifier.size(16.dp))
        PlaybackRow(
          playing = viewState.playing,
          showPreviousNext = viewState.showPreviousNextButtons,
          onPlayClick = onPlayClick,
          onRewindClick = onRewindClick,
          onFastForwardClick = onFastForwardClick,
          onSkipToPrevious = onSkipToPrevious,
          onSkipToNext = onSkipToNext,
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      PlayerActionsBar(
        speedText = speedText,
        speedActive = viewState.playbackSpeed != 1.0f,
        sleepTimerState = viewState.sleepTimerState,
        onSpeedClick = onSpeedChangeClick,
        onBookmarksClick = onBookmarkClick,
        onHistoryClick = onHistoryClick,
        onSleepClick = onSleepTimerClick,
        applyNavigationBarsPadding = false,
      )
      Spacer(modifier = Modifier.weight(0.3f))
    }
  }
}

@Composable
private fun PlayerHeader(
  title: String,
  author: String?,
  onClick: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 24.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    Text(
      text = title,
      fontSize = 24.sp,
      fontWeight = FontWeight.Medium,
      letterSpacing = (-0.12).sp,
      textAlign = TextAlign.Center,
      color = RavenTheme.colors.title,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (!author.isNullOrBlank()) {
      Spacer(Modifier.size(4.dp))
      Text(
        text = author,
        fontSize = 13.sp,
        letterSpacing = (-0.065).sp,
        textAlign = TextAlign.Center,
        color = RavenTheme.colors.support,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }
  }
}
