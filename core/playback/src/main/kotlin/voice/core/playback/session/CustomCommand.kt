package voice.core.playback.session

import android.os.Bundle
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import voice.core.playback.misc.Decibel

@Serializable
internal sealed interface CustomCommand {

  @Serializable
  data object ForceSeekToNext : CustomCommand

  @Serializable
  data object ForceSeekToPrevious : CustomCommand

  @Serializable
  data class SetSkipSilence(val skipSilence: Boolean) : CustomCommand

  @Serializable
  data class SetGain(val gain: Decibel) : CustomCommand

  @Serializable
  data object SeekBack : CustomCommand

  @Serializable
  data object SeekForward : CustomCommand

  @Serializable
  data object AddBookmark : CustomCommand

  @Serializable
  data object CycleSpeed : CustomCommand

  companion object {

    const val CUSTOM_COMMAND_ACTION = "voiceCommandAction"
    internal const val CUSTOM_COMMAND_EXTRA = "voiceCommandExtra"
    internal fun parse(
      command: SessionCommand,
      args: Bundle,
    ): CustomCommand? {
      if (command.customAction != CUSTOM_COMMAND_ACTION) {
        return null
      }
      // Controllers send the payload in the args bundle, while notification command buttons
      // carry it in the SessionCommand's own extras. Accept either.
      val json = args.getString(CUSTOM_COMMAND_EXTRA)
        ?: command.customExtras.getString(CUSTOM_COMMAND_EXTRA)
        ?: return null
      return Json.decodeFromString(serializer(), json)
    }
  }
}

internal fun MediaController.sendCustomCommand(command: CustomCommand) {
  val json = Json.encodeToString(CustomCommand.serializer(), command)
  sendCustomCommand(
    SessionCommand(CustomCommand.CUSTOM_COMMAND_ACTION, Bundle.EMPTY),
    Bundle().apply {
      putString(CustomCommand.CUSTOM_COMMAND_EXTRA, json)
    },
  )
}
