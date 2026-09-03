package voice.core.playback.session

import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import dev.zacsweers.metro.Inject
import voice.core.logging.api.Logger
import voice.core.playback.player.VoicePlayer

@Inject
class DynamicMediaButtons(
  private val player: VoicePlayer,
  private val layout: MediaButtonLayout,
) {

  private var session: MediaLibrarySession? = null
  private var androidAutoControllers = 0

  fun attachTo(session: MediaLibrarySession) {
    this.session = session
    player.addListener(
      object : Player.Listener {
        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
          refresh()
        }
      },
    )
  }

  fun onAndroidAutoConnected() {
    androidAutoControllers++
    refresh()
  }

  fun onAndroidAutoDisconnected() {
    androidAutoControllers = (androidAutoControllers - 1).coerceAtLeast(0)
    refresh()
  }

  private fun refresh() {
    val session = session ?: return
    val androidAutoConnected = androidAutoControllers > 0
    Logger.d("refreshing media buttons, androidAutoConnected=$androidAutoConnected")
    session.setMediaButtonPreferences(
      layout.buttons(player.playbackParameters.speed, androidAutoConnected = androidAutoConnected),
    )
  }
}
