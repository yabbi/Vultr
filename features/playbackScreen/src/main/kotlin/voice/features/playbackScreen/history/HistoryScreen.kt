package voice.features.playbackScreen.history
import voice.core.ui.RavenTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.material3.Icon
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import voice.core.data.ListeningHistoryAction
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

@ContributesTo(AppScope::class)
interface HistoryGraph {
  val historyViewModelFactory: HistoryViewModel.Factory
}

@Composable
fun HistorySheetContent(
  viewState: HistoryViewState,
  onDelete: (voice.core.data.ListeningSession.Id) -> Unit,
  onEntryClick: (voice.core.data.ListeningSession.Id) -> Unit = {},
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .navigationBarsPadding(),
  ) {
    Text(
      text = stringResource(StringsR.string.history_title),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 24.dp),
      fontSize = 18.sp,
      fontWeight = FontWeight.Medium,
      letterSpacing = (-0.09).sp,
      textAlign = TextAlign.Center,
      color = RavenTheme.colors.title,
    )
    if (viewState.days.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = stringResource(StringsR.string.history_empty),
          fontSize = 14.sp,
          color = RavenTheme.colors.caption,
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        viewState.days.forEach { day ->
          item(key = "header-${day.date}") {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = dayLabel(day.date),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.06).sp,
                color = RavenTheme.colors.title,
              )
              Text(
                text = daySummary(day),
                fontSize = 12.sp,
                letterSpacing = (-0.06).sp,
                color = RavenTheme.colors.caption,
              )
            }
          }
          items(day.entries, key = { it.id.value }) { entry ->
            HistoryRow(
              entry = entry,
              onClick = { onEntryClick(entry.id) },
              onDelete = { onDelete(entry.id) },
            )
          }
        }
      }
    }
  }
}

@Composable
private fun HistoryRow(
  entry: HistoryEntryViewState,
  onClick: () -> Unit,
  onDelete: () -> Unit,
) {
  var showPopup by remember { mutableStateOf(false) }
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(6.dp),
    color = RavenTheme.colors.bgTertiary,
  ) {
    Row(
      modifier = Modifier.padding(8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = buildString {
              if (entry.chapterName != null) {
                append(entry.chapterName)
                append(" - ")
              }
              append(entry.positionText)
            },
            modifier = Modifier.weight(1f, fill = false),
            fontSize = 12.sp,
            letterSpacing = (-0.06).sp,
            color = RavenTheme.colors.subTitle,
          )
          Text(
            text = formatClock(entry.time),
            modifier = Modifier.padding(start = 8.dp),
            fontSize = 12.sp,
            letterSpacing = (-0.06).sp,
            color = RavenTheme.colors.subTitle,
          )
        }
        Text(
          text = buildString {
            append(actionLabel(entry.action))
            if (entry.listenedMs >= 60_000L) {
              append(" · ")
              append(stringResource(StringsR.string.history_listened, formatDuration(entry.listenedMs)))
            }
            append(" · ")
            append(entry.globalPositionText)
          },
          fontSize = 12.sp,
          letterSpacing = (-0.06).sp,
          color = RavenTheme.colors.caption,
        )
      }
      Box {
        Icon(
          painter = painterResource(UiR.drawable.ic_mage_dots),
          contentDescription = null,
          modifier = Modifier
            .size(24.dp)
            .clickable { showPopup = true },
          tint = RavenTheme.colors.icon,
        )
        if (showPopup) {
          Popup(
            alignment = Alignment.TopEnd,
            offset = IntOffset(0, 80),
            onDismissRequest = { showPopup = false },
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = RavenTheme.colors.bgModal,
              shadowElevation = 4.dp,
              border = androidx.compose.foundation.BorderStroke(1.dp, RavenTheme.colors.borderAvg),
            ) {
              Text(
                text = stringResource(StringsR.string.delete),
                modifier = Modifier
                  .width(200.dp)
                  .clickable {
                    showPopup = false
                    onDelete()
                  }
                  .padding(10.dp),
                fontSize = 14.sp,
                letterSpacing = (-0.07).sp,
                color = RavenTheme.colors.subTitle,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun actionLabel(action: ListeningHistoryAction?): String {
  val resId = when (action) {
    ListeningHistoryAction.Played -> StringsR.string.history_action_played
    ListeningHistoryAction.Paused -> StringsR.string.history_action_paused
    ListeningHistoryAction.Jumped -> StringsR.string.history_action_jumped
    ListeningHistoryAction.SkippedToChapter -> StringsR.string.history_action_skipped_to_chapter
    ListeningHistoryAction.NewChapter -> StringsR.string.history_action_new_chapter
    ListeningHistoryAction.SleepTimer -> StringsR.string.history_action_sleep_timer
    null -> return ""
  }
  return stringResource(resId)
}

@Composable
private fun dayLabel(date: LocalDate): String {
  val today = LocalDate.now()
  return when {
    date == today -> stringResource(StringsR.string.bookmark_today)
    date == today.minusDays(1) -> stringResource(StringsR.string.bookmark_yesterday)
    date.year == today.year -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
    else -> date.format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", Locale.getDefault()))
  }
}

@Composable
private fun daySummary(day: HistoryDayViewState): String {
  val range = if (day.firstTime == day.lastTime) {
    formatClock(day.firstTime)
  } else {
    "${formatClock(day.firstTime)} – ${formatClock(day.lastTime)}"
  }
  return "$range · ${stringResource(StringsR.string.history_listened, formatDuration(day.totalListenedMs))}"
}

private fun formatClock(time: LocalTime): String {
  return time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
}

@Composable
private fun formatDuration(ms: Long): String {
  val duration = Duration.ofMillis(ms)
  val hours = duration.toHours()
  val minutes = duration.toMinutesPart()
  return when {
    hours > 0 && minutes > 0 -> stringResource(StringsR.string.history_duration_hours_minutes, hours, minutes)
    hours > 0 -> stringResource(StringsR.string.history_duration_hours, hours)
    minutes > 0 -> stringResource(StringsR.string.history_duration_minutes, minutes)
    else -> stringResource(StringsR.string.history_duration_under_minute)
  }
}
