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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import voice.core.ui.theme.PrewarmCoverAccent
import voice.core.data.BookId
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.core.ui.R as UiR

@Composable
internal fun GridBooks(
  entries: List<Pair<BookId, State<BookOverviewItemViewState>>>,
  columns: Int,
  currentBookId: BookId?,
  isPlaying: Boolean,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  onPlayClick: (BookId) -> Unit,
  showPermissionBugCard: Boolean,
  onPermissionBugCardClick: () -> Unit,
) {
  LazyVerticalGrid(
    modifier = Modifier.fillMaxSize(),
    columns = GridCells.Fixed(columns),
    verticalArrangement = Arrangement.spacedBy(24.dp),
    horizontalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
  ) {
    if (showPermissionBugCard) {
      item(span = { GridItemSpan(maxLineSpan) }) {
        PermissionBugCard(onPermissionBugCardClick)
      }
    }
    groupBooksByMonth(entries).forEach { (month, monthBooks) ->
      item(
        span = { GridItemSpan(maxLineSpan) },
        key = "header-$month",
        contentType = "header",
      ) {
        Text(
          text = month,
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
        GridBook(
          book = book,
          onBookClick = onBookClick,
          onBookLongClick = onBookLongClick,
          onPlayClick = onPlayClick,
          playing = book.id == currentBookId && isPlaying,
        )
      }
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
      Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.systemBars))
    }
  }
}

@Composable
internal fun GridBook(
  book: BookOverviewItemViewState,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  playing: Boolean = false,
  onPlayClick: ((BookId) -> Unit)? = null,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .combinedClickable(
        onClick = { onBookClick(book.id) },
        onLongClick = { onBookLongClick(book.id) },
      ),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1f)
        .clip(RoundedCornerShape(4.dp)),
      contentAlignment = Alignment.Center,
    ) {
      PrewarmCoverAccent(book.cover)
      AsyncImage(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f),
        contentScale = ContentScale.Crop,
        model = book.cover?.file,
        placeholder = painterResource(id = UiR.drawable.album_art),
        error = painterResource(id = UiR.drawable.album_art),
        contentDescription = null,
      )
      if (onPlayClick != null) {
        PlayBadge(
          playing = playing,
          onClick = { onPlayClick(book.id) },
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(8.dp),
        )
      }
    }

    Spacer(Modifier.height(4.dp))

    Text(
      text = book.name,
      fontSize = 14.sp,
      lineHeight = 16.sp,
      fontWeight = FontWeight.Medium,
      letterSpacing = (-0.07).sp,
      color = MaterialTheme.colorScheme.onSurface,
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )

    book.author?.let {
      Text(
        text = it,
        fontSize = 10.sp,
        letterSpacing = (-0.05).sp,
        color = RavenTheme.colors.support,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
    }

    if (book.progress > 0f) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        LinearProgressIndicator(
          progress = { book.progress },
          modifier = Modifier
            .weight(1F)
            .height(4.dp)
            .clip(MaterialTheme.shapes.small),
          drawStopIndicator = {},
        )
        Text(
          text = book.remainingTime,
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(start = 8.dp),
        )
      }
    }
  }
}

@Composable
internal fun PlayBadge(
  playing: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.size(22.dp),
    shape = RoundedCornerShape(2.dp),
    color = Color(0xFF696969),
    onClick = onClick,
  ) {
    Box(contentAlignment = Alignment.Center) {
      if (playing) {
        PlayingIndicator(modifier = Modifier.size(14.dp))
      } else {
        Icon(
          painter = painterResource(UiR.drawable.ic_mage_heaphone),
          contentDescription = null,
          tint = Color.White,
          modifier = Modifier.size(14.dp),
        )
      }
    }
  }
}

@Composable
internal fun gridColumnCount(): Int = 2

@Composable
@Preview(widthDp = 200)
private fun GridBookPreviewWithProgress() {
  GridBook(BookOverviewPreviewParameterProvider().book().copy(progress = 0.66f), {}, {})
}

@Composable
@Preview(widthDp = 200)
private fun GridBookPreviewWithoutProgress() {
  GridBook(BookOverviewPreviewParameterProvider().book().copy(progress = 0f), {}, {})
}
