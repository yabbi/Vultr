package voice.features.playbackScreen

import android.content.Intent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.RavenTheme
import voice.features.playbackScreen.history.HistoryGraph
import voice.features.playbackScreen.history.HistorySheetContent
import voice.features.playbackScreen.view.AddBookmarkDialog
import voice.features.playbackScreen.view.BookPlayView
import voice.features.playbackScreen.view.BookmarksBottomSheet
import voice.core.ui.theme.CoverAccentTheme
import voice.features.playbackScreen.view.EditBookmarkSheet
import voice.core.ui.theme.rememberCoverAccent
import voice.features.sleepTimer.SleepTimerDialog
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookPlayScreen(bookId: BookId) {
  val viewModel = retain(bookId.value) {
    rootGraphAs<BookPlayGraph>()
      .bookPlayViewModelFactory
      .create(bookId)
  }
  val context = LocalContext.current
  val snackbarHostState = remember { SnackbarHostState() }
  val dialogState = viewModel.dialogState.value
  val viewState = viewModel.viewState()
    ?: return
  val bookmarkAddedMessage = stringResource(StringsR.string.bookmark_added)
  val batteryOptimizationMessage = stringResource(StringsR.string.battery_optimization_rationale)
  val batteryOptimizationAction = stringResource(StringsR.string.battery_optimization_action)
  LaunchedEffect(viewModel) {
    viewModel.viewEffects.collect { viewEffect ->
      when (viewEffect) {
        BookPlayViewEffect.BookmarkAdded -> {
          snackbarHostState.showSnackbar(message = bookmarkAddedMessage)
        }
        BookPlayViewEffect.RequestIgnoreBatteryOptimization -> {
          val result = snackbarHostState.showSnackbar(
            message = batteryOptimizationMessage,
            duration = SnackbarDuration.Long,
            actionLabel = batteryOptimizationAction,
          )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.onBatteryOptimizationRequested()
          }
        }
        is BookPlayViewEffect.ExportBookmarks -> {
          val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, viewEffect.text)
          }
          context.startActivity(Intent.createChooser(sendIntent, "Export Bookmarks"))
        }
      }
    }
  }
  CoverAccentTheme(accent = rememberCoverAccent(viewState.cover)) {
    BookPlayView(
      viewState,
      onPlayClick = viewModel::playPause,
      onFastForwardClick = viewModel::fastForward,
      onRewindClick = viewModel::rewind,
      onSeek = viewModel::seekTo,
      onTimeDisplayClick = viewModel::cycleTimeDisplay,
      onBookmarkClick = viewModel::onBookmarkClick,
      onBookmarkLongClick = viewModel::onBookmarkLongClick,
      onAddBookmarkClick = viewModel::onAddBookmarkFromSheet,
      onHistoryClick = viewModel::onHistoryClick,
      onSkipSilenceClick = viewModel::toggleSkipSilence,
      onSleepTimerClick = viewModel::toggleSleepTimer,
      onVolumeBoostClick = viewModel::onVolumeGainIconClick,
      onSpeedChangeClick = viewModel::onPlaybackSpeedIconClick,
      onCloseClick = viewModel::onCloseClick,
      onSkipToNext = viewModel::next,
      onSkipToPrevious = viewModel::previous,
      onCurrentChapterClick = viewModel::onCurrentChapterClick,
      onBookDetailsClick = viewModel::onBookDetailsClick,
      // Two-pane only when the viewport is wider than tall (tablet landscape / phone landscape).
      // Portrait tablets use the same stacked layout as phones for feature parity.
      useLandscapeLayout = with(LocalConfiguration.current) { screenWidthDp > screenHeightDp },
      snackbarHostState = snackbarHostState,
    )
    if (dialogState != null) {
      when (dialogState) {
        is BookPlayDialogViewState.SpeedDialog -> {
          SpeedDialog(dialogState, viewModel)
        }
        is BookPlayDialogViewState.VolumeGainDialog -> {
          VolumeGainDialog(dialogState, viewModel)
        }
        is BookPlayDialogViewState.SelectChapterDialog -> {
          SelectChapterDialog(dialogState, viewModel)
        }
        is BookPlayDialogViewState.SleepTimer -> {
          SleepTimerDialog(
            viewState = dialogState.viewState,
            onDismiss = viewModel::dismissDialog,
            onIncrementSleepTime = viewModel::incrementSleepTime,
            onDecrementSleepTime = viewModel::decrementSleepTime,
            onAcceptSleepTime = viewModel::onAcceptSleepTime,
            onAcceptSleepAtEndOfChapter = viewModel::onAcceptSleepAtEndOfChapter,
          )
        }
      }
    }
    if (viewModel.showHistorySheet.value) {
      val historyViewModel = retain("history-${bookId.value}") {
        rootGraphAs<HistoryGraph>().historyViewModelFactory.create(bookId)
      }
      val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
      ModalBottomSheet(
        onDismissRequest = { viewModel.dismissHistorySheet() },
        sheetState = sheetState,
        containerColor = RavenTheme.colors.bgMain,
      ) {
        HistorySheetContent(
          viewState = historyViewModel.viewState(),
          onDelete = historyViewModel::onDelete,
          onEntryClick = { id ->
            historyViewModel.onEntryClick(id)
            viewModel.dismissHistorySheet()
          },
        )
      }
    }
    if (viewModel.showBookmarksSheet.value) {
      BookmarksBottomSheet(
        groups = viewModel.bookmarkGroups.value,
        onDismiss = viewModel::dismissBookmarksSheet,
        onAddBookmark = viewModel::onAddBookmarkFromSheet,
        onExportBookmarks = viewModel::exportBookmarks,
        onEditBookmark = viewModel::onEditBookmark,
        onDeleteBookmark = viewModel::deleteBookmark,
        onBookmarkClick = viewModel::onBookmarkItemClick,
      )
    }
    if (viewModel.showAddBookmarkDialog.value) {
      AddBookmarkDialog(
        onDismiss = viewModel::dismissAddBookmarkDialog,
        onSave = viewModel::saveNewBookmark,
      )
    }
    val editState = viewModel.editBookmarkState.value
    if (editState != null) {
      EditBookmarkSheet(
        state = editState,
        onDismiss = viewModel::dismissEditBookmark,
        onSave = viewModel::saveEditedBookmark,
      )
    }
  }
}

@ContributesTo(AppScope::class)
interface BookPlayGraph {
  val bookPlayViewModelFactory: BookPlayViewModel.Factory
}

@ContributesTo(AppScope::class)
interface BookPlayProvider {

  @Provides
  @IntoSet
  fun bookPlayNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.Playback> { key ->
    NavEntry(key) {
      BookPlayScreen(bookId = key.bookId)
    }
}
}
