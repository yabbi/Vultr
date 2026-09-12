package voice.core.data

import kotlinx.serialization.Serializable

@Serializable
public enum class PlaybackTimeDisplay {
  CHAPTER_TOTAL,
  CHAPTER_REMAINING,
  BOOK_REMAINING,
  ;

  public fun next(): PlaybackTimeDisplay {
    val values = entries
    return values[(ordinal + 1) % values.size]
  }
}
