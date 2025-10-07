package se.file14.procosmetics.noteblock;

import org.bukkit.event.Event;
import se.file14.procosmetics.api.cosmetic.music.NoteBlockSong;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class NoteBlockApiSupport {

    private static final String[] BASE_PACKAGES = {
            "com.xxmicloxx.NoteBlockAPI",
            "com.xxmicloxx.noteblockapi"
    };

    private static final NoteBlockApiSupport INSTANCE = new NoteBlockApiSupport();

    private final String basePackage;
    private final Class<?> songClass;
    private final Class<?> nbsDecoderClass;
    private final Class<?> soundCategoryClass;
    private final Class<?> positionSongPlayerClass;
    private final Class<?> songEndEventClass;

    private NoteBlockApiSupport() {
        String locatedPackage = null;
        Class<?> song = null;
        Class<?> decoder = null;
        Class<?> soundCategory = null;
        Class<?> positionPlayer = null;
        Class<?> endEvent = null;

        for (String candidate : BASE_PACKAGES) {
            try {
                Class<?> noteBlockApi = Class.forName(candidate + ".NoteBlockAPI");
                locatedPackage = candidate;
                song = Class.forName(candidate + ".model.Song");
                decoder = Class.forName(candidate + ".utils.NBSDecoder");
                soundCategory = Class.forName(candidate + ".model.SoundCategory");
                positionPlayer = Class.forName(candidate + ".songplayer.PositionSongPlayer");
                endEvent = Class.forName(candidate + ".event.SongEndEvent");
                noteBlockApi.getName(); // suppress unused warning
                break;
            } catch (ClassNotFoundException ignored) {
                // Try the next candidate
            }
        }

        this.basePackage = locatedPackage;
        this.songClass = song;
        this.nbsDecoderClass = decoder;
        this.soundCategoryClass = soundCategory;
        this.positionSongPlayerClass = positionPlayer;
        this.songEndEventClass = endEvent;
    }

    public static NoteBlockApiSupport getInstance() {
        return INSTANCE;
    }

    public boolean isAvailable() {
        return basePackage != null;
    }

    public NoteBlockSong wrapSong(Object song) {
        if (song == null) {
            return null;
        }
        return new NoteBlockSongImpl(song);
    }

    public NoteBlockSong loadSong(File file, Logger logger) {
        if (!isAvailable()) {
            return null;
        }
        try {
            Method parse = nbsDecoderClass.getMethod("parse", File.class);
            Object song = parse.invoke(null, file);
            return wrapSong(song);
        } catch (NoSuchMethodException | IllegalAccessException exception) {
            logger.log(Level.SEVERE, "Unable to access NoteBlockAPI NBSDecoder#parse", exception);
        } catch (InvocationTargetException exception) {
            logger.log(Level.SEVERE, "Failed to decode song file: " + file.getName(), exception.getTargetException());
        }
        return null;
    }

    public NoteBlockSongPlayer createPositionSongPlayer(NoteBlockSong song, Logger logger) {
        if (!isAvailable() || song == null) {
            return null;
        }
        try {
            Constructor<?> constructor = positionSongPlayerClass.getConstructor(songClass, soundCategoryClass);
            @SuppressWarnings({"unchecked", "rawtypes"})
            Class<? extends Enum> enumClass = (Class<? extends Enum>) soundCategoryClass.asSubclass(Enum.class);
            Object soundCategory = Enum.valueOf(enumClass, "RECORDS");
            Object handle = constructor.newInstance(song.unwrap(), soundCategory);
            return new NoteBlockSongPlayer(this, handle);
        } catch (ReflectiveOperationException exception) {
            logger.log(Level.SEVERE, "Failed to create NoteBlockAPI PositionSongPlayer", exception);
            return null;
        }
    }

    public boolean isSongEndEvent(Event event) {
        return songEndEventClass != null && songEndEventClass.isInstance(event);
    }

    public Object extractSongPlayer(Event event) {
        if (!isSongEndEvent(event)) {
            return null;
        }
        try {
            Method method = songEndEventClass.getMethod("getSongPlayer");
            return method.invoke(event);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    void invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (target == null) {
            return;
        }
        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            method.invoke(target, args);
        } catch (ReflectiveOperationException ignored) {
            // Swallow; behavior degrades gracefully
        }
    }
}

