package voice.core.playback.session

import android.content.Context
import android.os.Bundle
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionCommand
import com.google.common.collect.ImmutableList
import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json
import voice.core.playback.R
import voice.core.strings.R as StringsR

@Inject
class VoiceMediaNotificationProvider(private val context: Context) : DefaultMediaNotificationProvider(context) {

  init {
    // Use the Raven mark as the status-bar / cover-badge icon instead of the generic media glyph.
    setSmallIcon(R.drawable.ic_raven_notification)
  }

  override fun getMediaButtons(
    session: MediaSession,
    playerCommands: Player.Commands,
    customLayout: ImmutableList<CommandButton>,
    showPauseButton: Boolean,
  ): ImmutableList<CommandButton> {
    val defaults = super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
    val playPause = defaults.firstOrNull { it.playerCommand == Player.COMMAND_PLAY_PAUSE }

    // The phone media controls skip by the configured seek amount, matching the in-app player
    // and a steering-wheel tap. Android Auto keeps chapter navigation via the session's media
    // button preferences (see MediaButtonLayout), which it reads instead of these actions.
    val buttons = listOfNotNull(
      seekButton(
        playerCommand = Player.COMMAND_SEEK_BACK,
        icon = CommandButton.ICON_SKIP_BACK,
        iconRes = R.drawable.ic_fast_rewind,
        displayName = context.getString(StringsR.string.rewind),
      ),
      playPause,
      seekButton(
        playerCommand = Player.COMMAND_SEEK_FORWARD,
        icon = CommandButton.ICON_SKIP_FORWARD,
        iconRes = R.drawable.ic_fast_forward,
        displayName = context.getString(StringsR.string.fast_forward),
      ),
      bookmarkButton(),
    )

    buttons.take(MAX_COMPACT_BUTTONS).forEachIndexed { index, button ->
      // This shows the buttons in compact mode for Android < 13
      // https://github.com/VoiceAudiobook/Voice/issues/1904
      button.extras.putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, index)
    }
    return ImmutableList.copyOf(buttons)
  }

  private fun seekButton(
    playerCommand: Int,
    icon: Int,
    iconRes: Int,
    displayName: String,
  ): CommandButton {
    return CommandButton.Builder(icon)
      .setDisplayName(displayName)
      .setCustomIconResId(iconRes)
      .setPlayerCommand(playerCommand)
      .build()
  }

  private fun bookmarkButton(): CommandButton {
    val extras = Bundle().apply {
      putString(
        CustomCommand.CUSTOM_COMMAND_EXTRA,
        Json.encodeToString(CustomCommand.serializer(), CustomCommand.AddBookmark),
      )
    }
    return CommandButton.Builder(CommandButton.ICON_BOOKMARK_UNFILLED)
      .setDisplayName(context.getString(StringsR.string.add_bookmark))
      .setSessionCommand(SessionCommand(CustomCommand.CUSTOM_COMMAND_ACTION, extras))
      .build()
  }

  private companion object {
    const val MAX_COMPACT_BUTTONS = 3
  }
}
