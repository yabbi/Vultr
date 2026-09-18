package voice.features.bookOverview.views
import voice.core.ui.RavenTheme

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import voice.core.data.BookId
import voice.core.ui.ImmutableFile
import voice.core.ui.theme.PrewarmCoverAccent
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.core.ui.R as UiR

@Composable
internal fun ListBooks(
  entries: List<Pair<BookId, State<BookOverviewItemViewState>>>,
  currentBookId: BookId?,
  isPlaying: Boolean,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  onPlayClick: (BookId) -> Unit,
  onMenuClick: (BookId) -> Unit,
  showPermissionBugCard: Boolean,
  onPermissionBugCardClick: () -> Unit,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(top = 16.dp, start = 20.dp, end = 20.dp, bottom = 16.dp),
  ) {
    if (showPermissionBugCard) {
      item {
        PermissionBugCard(onPermissionBugCardClick)
      }
    }
    groupBooksByMonth(entries).forEach { (month, monthBooks) ->
      item(key = "header-$month", contentType = "header") {
        Text(
          text = month,
          modifier = Modifier.padding(vertical = 4.dp),
          fontSize = 16.sp,
          letterSpacing = (-0.08).sp,
          color = RavenTheme.colors.subTitle,
        )
      }
      items(
        items = monthBooks,
        key = { (bookId, _) -> bookId.value },
        contentType = { "item" },
      ) { (_, bookState) ->
        val book = bookState.value
        ListBookRow(
          book = book,
          onBookClick = onBookClick,
          onBookLongClick = onBookLongClick,
          onPlayClick = onPlayClick,
          onMenuClick = onMenuClick,
          playing = book.id == currentBookId && isPlaying,
        )
      }
    }
    item {
      Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
  }
}

@Composable
internal fun ListBookRow(
  book: BookOverviewItemViewState,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  playing: Boolean = false,
  onPlayClick: ((BookId) -> Unit)? = null,
  onMenuClick: ((BookId) -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = { onBookClick(book.id) },
        onLongClick = { onBookLongClick(book.id) },
      ),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(contentAlignment = Alignment.BottomStart) {
        CoverImage(book.cover)
        if (onPlayClick != null) {
          PlayBadge(
            playing = playing,
            onClick = { onPlayClick(book.id) },
            modifier = Modifier.padding(6.dp),
          )
        }
      }

      Column(
        Modifier
          .padding(start = 12.dp)
          .weight(1f),
      ) {
        if (book.author != null) {
          Text(
            text = book.author.toUpperCase(LocaleList.current),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )
        }

        Text(
          text = book.name,
          style = MaterialTheme.typography.titleSmall,
          lineHeight = 16.sp,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 2,
        )

        Row(
          modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = book.remainingTime,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
          )

          if (book.progress > 0f) {
            Text(
              text = "${(book.progress * 100).toInt()}%",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              maxLines = 1,
            )
          }
        }
      }

      if (onMenuClick != null) {
        IconButton(onClick = { onMenuClick(book.id) }) {
          Icon(
            painter = painterResource(UiR.drawable.ic_mage_dots),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = RavenTheme.colors.icon,
          )
        }
      }
    }

    if (book.progress > 0.05f) {
      LinearProgressIndicator(
        progress = { book.progress },
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 4.dp)
          .clip(MaterialTheme.shapes.small)
          .height(4.dp),
        color = MaterialTheme.colorScheme.primary,
        trackColor = MaterialTheme.colorScheme.surfaceVariant,
        drawStopIndicator = {},
      )
    }
  }
}

@Composable
private fun CoverImage(cover: ImmutableFile?) {
  PrewarmCoverAccent(cover)
  AsyncImage(
    modifier = Modifier
      .size(76.dp)
      .clip(RoundedCornerShape(2.dp)),
    model = cover?.file,
    placeholder = painterResource(id = UiR.drawable.album_art),
    error = painterResource(id = UiR.drawable.album_art),
    contentScale = ContentScale.Crop,
    contentDescription = null,
  )
}

@Composable
@Preview
private fun ListBookRowPreviewWithProgress() {
  ListBookRow(BookOverviewPreviewParameterProvider().book().copy(progress = 0.6f), {}, {})
}

@Composable
@Preview
private fun ListBookRowPreviewWithoutProgress() {
  ListBookRow(BookOverviewPreviewParameterProvider().book().copy(progress = 0f), {}, {})
}
