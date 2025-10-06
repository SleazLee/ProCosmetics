# ProCosmetics

ProCosmetics is a cosmetics plugin for Minecraft servers that helps you monetize in an EULA-friendly way and make
your server more fun with engaging cosmetic features. It gives players many visual options and customization choices,
and works entirely without requiring a resource pack. Perfect for hub and lobby servers, ProCosmetics can also be set up
to work with survival and other server types. The plugin includes arrow effects, balloons, banners, death effects,
emotes, gadgets, miniatures, morphs, mounts, music, particle effects, pets, and statuses, giving players endless ways to
express themselves and stand out on your server.

ProCosmetics currently supports Spigot/Paper 1.20.6 and newer, and is maintained to ensure compatibility with each new
release. To keep the codebase fresh and take advantage of the newest features, we may drop support for older versions
when necessary.

## Documentation

Full documentation is available on our [Wiki](https://github.com/File14/ProCosmetics/wiki).

For support, join our [Discord server](https://discord.gg/ERVgpfg).

## Official Builds

Official and stable releases can be found on:
[SpigotMC](https://www.spigotmc.org).

## How to compile

1. ### Set up Spigot dependencies
   Use [BuildTools](https://www.spigotmc.org/wiki/buildtools/#wikiPage) to create a remapped Spigot jar for every latest
   Minecraft version (ex: 1.20.6, not 1.20) the plugin currently supports. Alternatively, run
   `bash install.sh` to install the dependencies into your local Maven/Gradle cache.

2. ### Build the plugin
   Build with your preferred tool:

   - **Maven**: `mvn -B package` (artifacts are produced in `plugin/target/`).
   - **Gradle**: `./gradlew clean build obfuscate` (artifacts are produced in `build/libs/`).

## Update guide

The latest development cycle introduced Folia support and a Maven build option. Existing installations should review the
changes below before updating:

- **Folia-aware scheduler**: All synchronous, asynchronous, and region-sensitive tasks now go through a shared
  `Scheduler` utility that automatically detects Folia. The helper transparently chooses between Folia's region/async
  schedulers and the traditional Bukkit scheduler, ensuring features like treasure animations and cosmetic behaviors run
  safely on both platforms. Custom extensions should also migrate to this API (see
  `core/src/main/java/se/file14/procosmetics/util/Scheduler.java`).
- **Scheduler integration across modules**: Every cosmetic implementation, runnable helper, and treasure animation now
  delegates to `AbstractRunnable`/`Scheduler`. If you maintain custom modules, replace direct Bukkit scheduling calls
  (`Bukkit.getScheduler()`, `BukkitRunnable`, etc.) with the new helper to stay Folia-compatible.
- **Plugin metadata**: Folia compatibility is declared in `plugin.yml` so servers running Folia automatically enable the
  optimized schedulers. No configuration toggle is required anymore.
- **Build system**: A Maven multi-module build sits alongside the original Gradle scripts. The Maven reactor mirrors the
  Gradle project layout (`api`, `core`, `v1_20`, `v1_21`, and `plugin`) and the plugin module shades dependencies while
  filtering resources. You can continue using Gradle or switch to Maven depending on your infrastructure.
- **Resource layout**: Plugin descriptors (`plugin.yml`, locales, etc.) live under the new `plugin` module so both Gradle
  and Maven builds package the same assets.

### NoteBlockAPI on Folia

ProCosmetics bundles a fork of **NoteBlockAPI** and uses it extensively for the entire music cosmetic category. The
library powers song decoding (`songs/*.nbs` files) and playback, as seen in
`MusicTypeImpl`/`MusicImpl` and the plugin bootstrap where the API is initialised and shut down.【F:core/src/main/java/se/file14/procosmetics/cosmetic/music/MusicTypeImpl.java†L1-L84】【F:core/src/main/java/se/file14/procosmetics/cosmetic/music/MusicImpl.java†L1-L125】【F:core/src/main/java/se/file14/procosmetics/ProCosmeticsPlugin.java†L1-L176】

Because Folia refuses to load plugins that are not explicitly marked as compatible, the upstream NoteBlockAPI release
(`NoteBlockAPI v1.6.5-SNAPSHOT`) will be rejected during startup. You have three practical options when targeting Folia:

1. **Ship a Folia-capable fork** – Create a fork of NoteBlockAPI, add the Folia metadata (and any scheduler changes that
   may be required), and depend on that fork. This keeps all music cosmetics functional.
2. **Shade the fork directly** – We already shade our dependency; updating it to a Folia-ready fork avoids the need to
   install NoteBlockAPI as a separate plugin on the server. Follow the steps below once your forked jar is ready.
3. **Disable music cosmetics** – Removing NoteBlockAPI will prevent music cosmetics from loading, so if you cannot
   maintain a fork you should disable or remove the entire music category from your configuration to avoid runtime
   errors.

For production servers we strongly recommend option 2 so players retain access to the music cosmetics while remaining
Folia-compliant.

#### Shading your Folia-ready NoteBlockAPI fork

1. Build your fork and copy the resulting jar into `libs/NoteBlockAPI-folia.jar` in the repository root (create the
   `libs` directory if it does not exist). Keeping the jar in a shared location lets both the Gradle and Maven builds
   reference the same artifact.
2. Update the Gradle dependencies so every module resolves against the local jar:
   - In `api/build.gradle.kts`, replace the existing NoteBlockAPI `compileOnly` entry with
     ```kotlin
     compileOnly(files(rootProject.file("libs/NoteBlockAPI-folia.jar")))
     ```
   - In `core/build.gradle.kts`, replace the NoteBlockAPI `implementation` entry with
     ```kotlin
     implementation(files(rootProject.file("libs/NoteBlockAPI-folia.jar")))
     ```
   These statements keep the jar shaded into the final plugin artifact, just like the current remote dependency.
3. If you use the Maven build, install the jar into your local Maven repository so the `api` and `core` modules can pull
   it in:
   ```bash
   mvn install:install-file \ 
     -Dfile=libs/NoteBlockAPI-folia.jar \ 
     -DgroupId=se.file14.folia \ 
     -DartifactId=NoteBlockAPI \ 
     -Dversion=1.6.5-folia \ 
     -Dpackaging=jar
   ```
   Afterwards, update both `api/pom.xml` and `core/pom.xml` to depend on `se.file14.folia:NoteBlockAPI:1.6.5-folia`
   instead of the temporary JitPack coordinate.

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE).md for full details.