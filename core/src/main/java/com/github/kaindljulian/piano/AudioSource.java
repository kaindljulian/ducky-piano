package com.github.kaindljulian.piano;

import java.util.function.Consumer;

/** Asks the player to let the game listen to audio from other apps, on platforms where that needs permission. */
public interface AudioSource {
    /** Shows the system prompts, then calls {@code done} on any thread with whether the player allowed it. */
    void request(Consumer<Boolean> done);
}
