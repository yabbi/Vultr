package voice.features.bookOverview.details

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.navigation3.runtime.NavEntry
import coil.compose.AsyncImage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.RavenTheme
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.ui.R as UiR

@ContributesTo(AppScope::class)
interface EditBookGraph {
  val editBookViewModelFactory: EditBookViewModel.Factory
}

@ContributesTo(AppScope::class)
interface EditBookProvider {

  @Provides
  @IntoSet
  fun editBookNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.EditBook> { key ->
    NavEntry(key) {
      EditBookScreen(bookId = key.bookId)
    }
  }
}

@Composable
fun EditBookScreen(bookId: BookId) {
  val viewModel = retain(bookId.value) {
    rootGraphAs<EditBookGraph>().editBookViewModelFactory.create(bookId)
  }
  val form = viewModel.form.value ?: return
  EditBookContent(
    form = form,
    onBack = viewModel::onBack,
    onSave = viewModel::save,
    onPickCover = viewModel::onPickCover,
    onDownloadCover = viewModel::onDownloadCover,
    onAdjustCrop = viewModel::onAdjustCrop,
  )
}

@Composable
private fun EditBookContent(
  form: EditBookForm,
  onBack: () -> Unit,
  onSave: (String, String, String, String) -> Unit,
  onPickCover: (Uri) -> Unit,
  onDownloadCover: () -> Unit,
  onAdjustCrop: () -> Unit,
) {
  var title by remember { mutableStateOf(form.title) }
  var author by remember { mutableStateOf(form.author) }
  var date by remember { mutableStateOf(form.date) }
  var description by remember { mutableStateOf(form.description) }
  var showCoverMenu by remember { mutableStateOf(false) }

  val galleryLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.GetContent(),
    onResult = { uri -> if (uri != null) onPickCover(uri) },
  )

  Column(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding(),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onBack) {
        Icon(
          painter = painterResource(UiR.drawable.ic_mage_arrow_left),
          contentDescription = "Back",
          modifier = Modifier.size(24.dp),
          tint = RavenTheme.colors.title,
        )
      }
      Spacer(Modifier.width(8.dp))
      Text(
        text = "Edit Book",
        fontSize = 24.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.12).sp,
        color = RavenTheme.colors.title,
      )
    }

    Column(
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp),
    ) {
      Spacer(Modifier.height(8.dp))
      Box(
        modifier = Modifier
          .fillMaxWidth(0.55f)
          .align(Alignment.CenterHorizontally),
      ) {
        AsyncImage(
          modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { showCoverMenu = true },
          model = form.cover?.file,
          placeholder = painterResource(UiR.drawable.album_art),
          error = painterResource(UiR.drawable.album_art),
          contentScale = ContentScale.Crop,
          contentDescription = "Edit cover",
        )
        if (showCoverMenu) {
          Popup(
            alignment = Alignment.BottomCenter,
            onDismissRequest = { showCoverMenu = false },
          ) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = RavenTheme.colors.bgModal,
              shadowElevation = 4.dp,
              border = BorderStroke(1.dp, RavenTheme.colors.borderAvg),
            ) {
              Column(modifier = Modifier.padding(vertical = 4.dp)) {
                CoverMenuItem("Pick from Gallery") {
                  showCoverMenu = false
                  galleryLauncher.launch("image/*")
                }
                CoverMenuItem("Download Cover") {
                  showCoverMenu = false
                  onDownloadCover()
                }
                if (form.cover != null) {
                  CoverMenuItem("Adjust Crop") {
                    showCoverMenu = false
                    onAdjustCrop()
                  }
                }
              }
            }
          }
        }
      }
      Spacer(Modifier.height(24.dp))
      EditField("Title", title) { title = it }
      Spacer(Modifier.height(16.dp))
      EditField("Author", author) { author = it }
      Spacer(Modifier.height(16.dp))
      EditField("Date", date) { date = it }
      Spacer(Modifier.height(16.dp))
      EditField("Description", description, singleLine = false) { description = it }
      Spacer(Modifier.height(24.dp))
    }

    Surface(
      modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .padding(horizontal = 20.dp, vertical = 12.dp)
        .height(48.dp)
        .clickable { onSave(title, author, date, description) },
      shape = RoundedCornerShape(12.dp),
      color = RavenTheme.colors.primary,
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Save Updates",
          color = Color.White,
          fontWeight = FontWeight.Medium,
          fontSize = 16.sp,
          letterSpacing = (-0.08).sp,
        )
      }
    }
  }
}

@Composable
private fun CoverMenuItem(
  label: String,
  onClick: () -> Unit,
) {
  Text(
    text = label,
    modifier = Modifier
      .width(180.dp)
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 12.dp),
    fontSize = 14.sp,
    letterSpacing = (-0.07).sp,
    color = RavenTheme.colors.subTitle,
  )
}

@Composable
private fun EditField(
  label: String,
  value: String,
  singleLine: Boolean = true,
  onValueChange: (String) -> Unit,
) {
  Column {
    Text(
      text = label,
      fontSize = 12.sp,
      letterSpacing = (-0.06).sp,
      color = RavenTheme.colors.caption,
    )
    Spacer(Modifier.height(4.dp))
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      modifier = Modifier.fillMaxWidth(),
      singleLine = singleLine,
      shape = RoundedCornerShape(12.dp),
    )
  }
}
