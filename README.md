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

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE).md for full details.