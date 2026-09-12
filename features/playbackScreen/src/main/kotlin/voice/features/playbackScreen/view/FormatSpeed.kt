package voice.features.playbackScreen.view

import java.text.DecimalFormat

// Shows up to two decimals, trimming trailing zeros: 1 → "1×", 1.5 → "1.5×", 1.15 → "1.15×".
private val speedFormat = DecimalFormat("0.##")

internal fun formatSpeed(speed: Float): String {
  return speedFormat.format(speed) + "×"
}
