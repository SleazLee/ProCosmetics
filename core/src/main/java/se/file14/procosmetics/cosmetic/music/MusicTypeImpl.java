package se.file14.procosmetics.cosmetic.music;

import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import se.file14.procosmetics.ProCosmeticsPlugin;
import se.file14.procosmetics.api.cosmetic.music.Music;
import se.file14.procosmetics.api.cosmetic.music.MusicBehavior;
import se.file14.procosmetics.api.cosmetic.music.MusicType;
import se.file14.procosmetics.api.cosmetic.music.NoteBlockSong;
import se.file14.procosmetics.api.cosmetic.registry.CosmeticCategory;
import se.file14.procosmetics.api.rarity.CosmeticRarity;
import se.file14.procosmetics.api.user.User;
import se.file14.procosmetics.cosmetic.CosmeticTypeImpl;
import se.file14.procosmetics.noteblock.NoteBlockApiSupport;

import java.io.File;
import java.util.function.Supplier;
import java.util.logging.Level;

public class MusicTypeImpl extends CosmeticTypeImpl<MusicType, MusicBehavior> implements MusicType {

    private final NoteBlockSong song;

    public MusicTypeImpl(String key,
                         CosmeticCategory<MusicType, MusicBehavior, ?> category,
                         Supplier<MusicBehavior> behaviorFactory,
                         boolean enabled,
                         boolean findable,
                         boolean purchasable,
                         int cost,
                         CosmeticRarity rarity,
                         ItemStack itemStack,
                         NoteBlockSong song) {
        super(key, category, behaviorFactory, enabled, findable, purchasable, cost, rarity, itemStack);
        this.song = song;
    }

    @Override
    protected Music createInstance(ProCosmeticsPlugin plugin, User user, MusicBehavior behavior) {
        return new MusicImpl(plugin, user, this, behavior);
    }

    @Override
    public @Nullable NoteBlockSong getSong() {
        return song;
    }

    public static class BuilderImpl extends CosmeticTypeImpl.BuilderImpl<MusicType, MusicBehavior, MusicType.Builder> implements MusicType.Builder {

        private NoteBlockSong song;

        public BuilderImpl(String key, CosmeticCategory<MusicType, MusicBehavior, ?> category) {
            super(key, category);
        }

        @Override
        protected MusicType.Builder self() {
            return this;
        }

        @Override
        public MusicType.Builder readFromConfig() {
            super.readFromConfig();
            loadSong();
            return this;
        }

        private void loadSong() {
            File file = PLUGIN.getDataFolder().toPath().resolve("songs").resolve(key + ".nbs").toFile();

            if (!file.exists()) {
                PLUGIN.getLogger().log(Level.WARNING, "Song file not found: " + file.getName()
                        + ". Ensure the file exists in the songs folder (case-sensitive).");
                return;
            }
            NoteBlockApiSupport support = NoteBlockApiSupport.getInstance();

            if (!support.isAvailable()) {
                PLUGIN.getLogger().log(Level.WARNING, "NoteBlockAPI is not present; skipping song " + file.getName() + '.');
                return;
            }

            NoteBlockSong decoded = support.loadSong(file, PLUGIN.getLogger());
            if (decoded == null) {
                PLUGIN.getLogger().log(Level.WARNING, "Failed to load song file: " + file.getName());
                return;
            }

            song = decoded;
        }

        @Override
        public MusicType.Builder song(NoteBlockSong song) {
            this.song = song;
            return this;
        }

        @Override
        public MusicType build() {
            return new MusicTypeImpl(key,
                    category,
                    factory,
                    enabled,
                    findable,
                    purchasable,
                    cost,
                    rarity,
                    itemStack,
                    song
            );
        }
    }
}