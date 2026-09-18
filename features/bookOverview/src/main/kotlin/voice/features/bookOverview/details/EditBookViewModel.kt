package voice.features.bookOverview.details

import android.net.Uri
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.ui.ImmutableFile
import voice.navigation.Destination
import voice.navigation.Navigator

@AssistedInject
class EditBookViewModel(
  private val repo: BookRepository,
  private val navigator: Navigator,
  @Assisted
  private val bookId: BookId,
) {

  private val scope = MainScope()
  private val _form = mutableStateOf<EditBookForm?>(null)
  val form: State<EditBookForm?> get() = _form

  init {
    scope.launch {
      repo.flow(bookId).filterNotNull().collect { book ->
        _form.value = EditBookForm(
          title = book.content.name,
          author = book.content.author.orEmpty(),
          date = book.content.year?.toString().orEmpty(),
          description = book.content.description.orEmpty(),
          cover = book.content.cover?.let(::ImmutableFile),
          accentColor = book.content.accentColor,
        )
      }
    }
  }

  fun onBack() {
    navigator.goBack()
  }

  fun save(
    title: String,
    author: String,
    date: String,
    description: String,
  ) {
    scope.launch {
      repo.updateBook(bookId) { content ->
        content.copy(
          name = title.trim().ifBlank { content.name },
          author = author.trim().ifBlank { null },
          year = date.trim().toIntOrNull(),
          description = description.trim().ifBlank { null },
        )
      }
    }
    navigator.goBack()
  }

  fun onPickCover(uri: Uri) {
    navigator.goTo(Destination.EditCover(bookId, uri))
  }

  fun onAdjustCrop() {
    val cover = form.value?.cover?.file ?: return
    navigator.goTo(Destination.EditCover(bookId, Uri.fromFile(cover), adjustExisting = true))
  }

  fun onAccentColorPicked(color: Int) {
    setAccentColor(color)
  }

  fun onResetAccentColor() {
    setAccentColor(null)
  }

  private fun setAccentColor(color: Int?) {
    scope.launch {
      repo.updateBook(bookId) { it.copy(accentColor = color) }
    }
  }

  fun onDownloadCover() {
    navigator.goTo(Destination.CoverFromInternet(bookId))
  }

  @AssistedFactory
  interface Factory {
    fun create(bookId: BookId): EditBookViewModel
  }
}
