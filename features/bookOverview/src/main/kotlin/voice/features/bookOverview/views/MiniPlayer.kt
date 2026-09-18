package voice.features.bookOverview.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import voice.core.ui.RavenTheme
import voice.core.ui.theme.CoverAccentTheme
import voice.core.ui.theme.rememberCoverAccent
import voice.features.bookOverview.overview.MiniPlayerViewState
import voice.core.ui.R as UiR

@Composable
internal fun MiniPlayer(
  viewState: MiniPlayerViewState,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
) {
  CoverAccentTheme(accent = rememberCoverAccent(viewState.cover, viewState.accentColor)) {
    MiniPlayerContent(viewState, onClick, onPlayClick)
  }
}

@Composable
private fun MiniPlayerContent(
  viewState: MiniPlayerViewState,
  onClick: () -> Unit,
  onPlayClick: () -> Unit,
) {
  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick),
    color = MaterialTheme.colorScheme.surfaceVariant,
    tonalElevation = 3.dp,
  ) {
    Column(Modifier.navigationBarsPadding()) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AsyncImage(
          modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(2.dp)),
          model = viewState.cover?.file,
          placeholder = painterResource(id = UiR.drawable.album_art),
          error = painterResource(id = UiR.drawable.album_art),
          contentScale = ContentScale.Crop,
          contentDescription = null,
        )
        Column(
          modifier = Modifier
            .weight(1F)
            .padding(horizontal = 16.dp),
        ) {
          Text(
            text = viewState.chapterTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Text(
            text = buildString {
              append(viewState.positionText)
              append("/ ")
              append(viewState.durationText)
              viewState.author?.let {
                append(" – ")
                append(it)
              }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Surface(
          modifier = Modifier
            .size(48.dp)
            .clickable(onClick = onPlayClick),
          shape = CircleShape,
          color = RavenTheme.colors.primary,
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              painter = painterResource(if (viewState.playing) UiR.drawable.ic_pause_filled else UiR.drawable.ic_play_filled),
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(24.dp),
            )
          }
        }
      }
      LinearProgressIndicator(
        progress = { viewState.progress },
        modifier = Modifier
          .fillMaxWidth()
          .height(4.dp)
          .padding(horizontal = 20.dp),
        drawStopIndicator = {},
      )
    }
  }
}
