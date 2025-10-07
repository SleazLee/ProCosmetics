package se.file14.procosmetics.api.cosmetic.music;

/**
 * Represents a handle to a NoteBlockAPI song without exposing the concrete
 * implementation. Implementations wrap the underlying NoteBlockAPI song
 * instance and allow the core module to interact with it reflectively.
 */
public interface NoteBlockSong {

    /**
     * Returns the underlying NoteBlockAPI song instance. The returned object is
     * intentionally untyped so that the Folia-compatible NoteBlockAPI fork can
     * change packages without breaking the API contract.
     *
     * @return the wrapped NoteBlockAPI song instance
     */
    Object unwrap();
}

