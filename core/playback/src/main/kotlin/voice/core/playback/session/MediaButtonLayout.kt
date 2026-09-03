package voice.core.playback.session

import android.content.Context
import android.os.Bundle
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json
import java.util.Locale
import voice.core.playback.R
import voice.core.strings.R as StringsR

@Inject
class MediaButtonLayout(private val context: Context) {

  // The platform session exposes a single set of custom actions that both the phone's system
  // media controls and Android Auto render, so the layout is swapped based on whether Auto is
  // connected rather than chosen per controller.
  fun buttons(
    speed: Float,
    androidAutoConnected: Boolean,
  ): List<CommandButton> {
    return if (androidAutoConnected) androidAutoButtons(speed) else phoneButtons()
  }

  private fun phoneButtons(): List<CommandButton> {
    return listOf(
      commandButton(
        command = CustomCommand.SeekBack,
        icon = CommandButton.ICON_SKIP_BACK,
        iconRes = R.drawable.ic_fast_rewind,
        displayName = context.getString(StringsR.string.rewind),
        slot = CommandButton.SLOT_BACK,
      ),
      commandButton(
        command = CustomCommand.SeekForward,
        icon = CommandButton.ICON_SKIP_FORWARD,
        iconRes = R.drawable.ic_fast_forward,
        displayName = context.getString(StringsR.string.fast_forward),
        slot = CommandButton.SLOT_FORWARD,
      ),
      bookmarkButton(),
    )
  }

  private fun androidAutoButtons(speed: Float): List<CommandButton> {
    return listOf(
      commandButton(
        command = CustomCommand.ForceSeekToPrevious,
        icon = CommandButton.ICON_PREVIOUS,
        iconRes = R.drawable.ic_skip_to_previous,
        displayName = context.getString(StringsR.string.previous_track),
        slot = CommandButton.SLOT_BACK,
      ),
      commandButton(
        command = CustomCommand.ForceSeekToNext,
        icon = CommandButton.ICON_NEXT,
        iconRes = R.drawable.ic_skip_to_next,
        displayName = context.getString(StringsR.string.next_track),
        slot = CommandButton.SLOT_FORWARD,
      ),
      bookmarkButton(),
      commandButton(
        command = CustomCommand.CycleSpeed,
        icon = SpeedCycle.icon(speed),
        displayName = context.getString(StringsR.string.playback_speed) +
          " " + String.format(Locale.getDefault(), "%.1fx", speed),
        slot = CommandButton.SLOT_OVERFLOW,
      ),
    )
  }

  private fun bookmarkButton(): CommandButton {
    return commandButton(
      command = CustomCommand.AddBookmark,
      icon = CommandButton.ICON_BOOKMARK_UNFILLED,
      displayName = context.getString(StringsR.string.add_bookmark),
      slot = CommandButton.SLOT_OVERFLOW,
    )
  }

  private fun commandButton(
    command: CustomCommand,
    icon: Int,
    displayName: String,
    slot: Int,
    iconRes: Int? = null,
  ): CommandButton {
    val extras = Bundle().apply {
      putString(
        CustomCommand.CUSTOM_COMMAND_EXTRA,
        Json.encodeToString(CustomCommand.serializer(), command),
      )
    }
    return CommandButton.Builder(icon)
      .setDisplayName(displayName)
      .apply { if (iconRes != null) setCustomIconResId(iconRes) }
      .setSessionCommand(SessionCommand(CustomCommand.CUSTOM_COMMAND_ACTION, extras))
      .setSlots(slot)
      .build()
  }
}
