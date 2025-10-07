package se.file14.procosmetics.noteblock;

import org.bukkit.Location;

public final class NoteBlockSongPlayer {

    private final NoteBlockApiSupport support;
    private final Object handle;

    NoteBlockSongPlayer(NoteBlockApiSupport support, Object handle) {
        this.support = support;
        this.handle = handle;
    }

    public void setTargetLocation(Location location) {
        support.invoke(handle, "setTargetLocation", new Class<?>[]{Location.class}, location);
    }

    public void setPlaying(boolean playing) {
        support.invoke(handle, "setPlaying", new Class<?>[]{boolean.class}, playing);
    }

    public void destroy() {
        support.invoke(handle, "destroy", new Class<?>[0]);
    }

    public Object unwrap() {
        return handle;
    }
}

