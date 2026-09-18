package voice.features.playbackScreen
import voice.core.ui.RavenTheme

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SelectChapterDialog(
  dialogState: BookPlayDialogViewState.SelectChapterDialog,
  viewModel: BookPlayViewModel,
) {
  ModalBottomSheet(
    sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    onDismissRequest = { viewModel.dismissDialog() },
    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding(),
    ) {
      Text(
        text = "Chapters",
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 24.dp),
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.09).sp,
        textAlign = TextAlign.Center,
        color = RavenTheme.colors.title,
      )
      val selectedIndex = dialogState.items.indexOfFirst { it.active }
      val initialFirstVisibleItemIndex = (selectedIndex - 1).coerceAtLeast(0)
      LazyColumn(
        state = rememberLazyListState(initialFirstVisibleItemIndex = initialFirstVisibleItemIndex),
      ) {
        items(dialogState.items) { chapter ->
          ChapterItem(
            chapter = chapter,
            onClick = { viewModel.onChapterClick(number = chapter.number) },
          )
        }
      }
    }
  }
}

@Composable
private fun ChapterItem(
  chapter: BookPlayDialogViewState.SelectChapterDialog.ItemViewState,
  onClick: () -> Unit,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 24.dp),
  ) {
    val bgColor = if (chapter.active) RavenTheme.colors.primaryFaint else Color.Transparent
    val textColor = if (chapter.active) RavenTheme.colors.primary else RavenTheme.colors.subTitle
    val numberColor = if (chapter.active) RavenTheme.colors.primary else Color(0xFF627193)
    val timeColor = if (chapter.active) RavenTheme.colors.primary else RavenTheme.colors.caption

    Surface(
      shape = RoundedCornerShape(8.dp),
      color = bgColor,
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clickable(onClick = onClick)
          .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = chapter.number.toString(),
          modifier = Modifier.width(24.dp),
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold,
          letterSpacing = (-0.06).sp,
          color = numberColor,
        )
        Text(
          text = chapter.name,
          modifier = Modifier.weight(1f),
          fontSize = 12.sp,
          letterSpacing = (-0.06).sp,
          color = textColor,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = "${chapter.time} (${chapter.duration})",
          fontSize = 10.sp,
          letterSpacing = (-0.05).sp,
          color = timeColor,
          textAlign = TextAlign.End,
        )
      }
    }
    HorizontalDivider(
      modifier = Modifier.padding(top = 8.dp),
      color = RavenTheme.colors.borderStrong.copy(alpha = 0.2f),
    )
  }
}
