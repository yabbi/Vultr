package voice.navigation

import android.content.Intent
import android.net.Uri
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import voice.core.common.serialization.UriSerializer
import voice.core.data.BookId

sealed interface Destination {

  @Serializable
  data class Playback(val bookId: BookId) : Compose {
    override val trackingName: String get() = "Playback"
  }

  @Serializable
  data class BookDetails(val bookId: BookId) : Compose {
    override val trackingName: String get() = "BookDetails"
  }

  @Serializable
  data class EditBook(val bookId: BookId) : Compose {
    override val trackingName: String get() = "EditBook"
  }

  @Serializable
  data class CoverFromInternet(val bookId: BookId) : Compose {
    override val trackingName: String get() = "CoverFromInternet"
  }

  data class Website(val url: String) : Destination

  @Serializable
  data class EditCover(
    val bookId: BookId,
    val cover:
    @Serializable(with = UriSerializer::class)
    Uri,
    val adjustExisting: Boolean = false,
  ) : Compose {
    override val trackingName: String get() = "EditCover"
  }

  data class Activity(val intent: Intent) : Destination

  @Serializable
  sealed interface Compose :
    Destination,
    NavKey {
    val trackingName: String
  }

  @Serializable
  data object Settings : Compose {
    override val trackingName: String get() = "Settings"
  }

  @Serializable
  data object Profile : Compose {
    override val trackingName: String get() = "Profile"
  }

  @Serializable
  data object DeveloperSettings : Compose {
    override val trackingName: String get() = "DeveloperSettings"
  }

  @Serializable
  data object BookOverview : Compose {
    override val trackingName: String get() = "BookOverview"
  }

  @Serializable
  data object FolderPicker : Compose {
    override val trackingName: String get() = "FolderPicker"
  }

  @Serializable
  data class SelectFolderType(
    val uri:
    @Serializable(with = UriSerializer::class)
    Uri,
    val origin: Origin,
  ) : Compose {
    override val trackingName: String = "SelectFolderType"
  }

  @Serializable
  data object OnboardingWelcome : Compose {
    override val trackingName: String get() = "OnboardingWelcome"
  }

  data object BatteryOptimization : Destination

  @Serializable
  data class AddContent(val origin: Origin) : Compose {
    override val trackingName: String = "AddContent"
  }
}
