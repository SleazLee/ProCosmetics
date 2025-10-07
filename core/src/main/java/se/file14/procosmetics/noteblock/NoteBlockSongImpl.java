package se.file14.procosmetics.noteblock;

import se.file14.procosmetics.api.cosmetic.music.NoteBlockSong;

final class NoteBlockSongImpl implements NoteBlockSong {

    private final Object handle;

    NoteBlockSongImpl(Object handle) {
        this.handle = handle;
    }

    @Override
    public Object unwrap() {
        return handle;
    }
}

