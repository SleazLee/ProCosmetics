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

[![bStats](https://bstats.org/signatures/bukkit/ProCosmetics.svg)](https://bstats.org/plugin/bukkit/ProCosmetics)  
*Figure: Shows real-time concurrent player counts across servers running the plugin. More detailed statistics and charts
are available on [bStats](https://bstats.org/plugin/bukkit/ProCosmetics/6408).*

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
   `bash install.sh` to put all dependencies on your local Gradle cache.

2. ### Build the plugin
   Build the project by executing the following tasks: `clean`, `build`, `obfuscate`. The jar file will be built in
   `build/libs/ProCosmetics.jar`.

## Folia support update guide

This plugin now ships with Folia-aware scheduling, while remaining compatible with traditional Bukkit/Paper servers.
When updating or backporting changes, use the checklist below to keep Folia support intact and to quickly verify that
new code paths remain safe:

1. **Scheduler utility**
   - All sync/async/region tasks should flow through `core/src/main/java/se/file14/procosmetics/util/Scheduler.java` or
     helper classes that delegate to it (for example `AbstractRunnable`).
   - Replace `Bukkit.getScheduler()*`, `BukkitRunnable`, and `runTask*` calls with the corresponding `Scheduler` method:
     - Global sync: `Scheduler.run`, `Scheduler.runLater`, `Scheduler.runTimer`.
     - Async: `Scheduler.runAsync`, `Scheduler.runAsyncLater`, `Scheduler.runAsyncTimer`.
     - Region/world-sensitive work: `Scheduler.run(location, ...)`, `runLater(location, ...)`, `runTimer(location, ...)`
       to ensure the task executes in the correct Folia region.
   - When you add new features, search for raw scheduler usage before merging. Folia requires at least 1 tick of delay
     for scheduled tasks; `Scheduler.runLater` already enforces this.

2. **Plugin descriptor**
   - Verify `src/main/resources/plugin.yml` contains `folia-supported: true` so Folia servers recognize the plugin.

3. **Dependencies**
   - `core/build.gradle.kts` declares the Folia API as a `compileOnly` dependency. Keep it up to date with upstream
     Folia versions if new APIs are required.

4. **AbstractRunnable tasks**
   - The shared runnable base class delegates `runTask`, `runTaskLater`, and `runTaskTimer` to `Scheduler`. Any new
     repeating animations or timed logic should extend or follow this pattern so the correct scheduler is chosen per
     environment.

5. **Region-sensitive actions**
   - When working with entities, blocks, or player contexts, capture a `Location` (or entity/player) and use the
     region-aware scheduler methods so Folia can execute the task in the correct region thread.

6. **Manual verification steps for future updates**
   - Before release, run a quick search for `getScheduler()` or `runTask` in the codebase to catch any direct Bukkit
     scheduler use that slipped in.
   - Smoke-test on both a Folia server and a standard Paper/Spigot server: equip/unequip cosmetics, trigger gadgets,
     open treasure animations, and watch for scheduler-related warnings or errors.
   - Confirm that async tasks are only doing non-Bukkit work; world interaction must be scheduled on the appropriate
     sync or region scheduler method.

Following these steps will keep Folia compatibility intact as the plugin evolves while preserving support for
traditional server platforms.

## License

This project is licensed under the GNU General Public License v3.0. See [LICENSE](LICENSE) for full details.