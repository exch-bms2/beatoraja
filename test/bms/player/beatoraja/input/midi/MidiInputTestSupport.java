package bms.player.beatoraja.input.midi;

import bms.player.beatoraja.input.BMSPlayerInputProcessor;

import java.util.List;

/** Creates a MIDI processor without discovering or opening host devices. */
public final class MidiInputTestSupport {
    private MidiInputTestSupport() {}

    public static MidiInputProcessor withoutDevices(BMSPlayerInputProcessor owner) {
        return new MidiInputProcessor(owner, List.of());
    }
}
