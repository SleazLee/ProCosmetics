package se.file14.procosmetics.util;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;
import se.file14.procosmetics.ProCosmeticsPlugin;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * Utility class for scheduling tasks, with support for Folia.
 *
 * <p>This class provides helper methods for running tasks in a Folia-aware way. The
 * Folia specific scheduler is used whenever the runtime provides the Folia scheduler API, otherwise
 * the Bukkit scheduler is used as a fallback.</p>
 */
public final class Scheduler {

    private static final boolean FOLIA_AVAILABLE;

    static {
        boolean available;
        try {
            Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            available = true;
        } catch (ClassNotFoundException exception) {
            available = false;
        }
        FOLIA_AVAILABLE = available;
    }

    private static ProCosmeticsPlugin plugin;
    private static boolean foliaEnabled;

    private Scheduler() {
    }

    /**
     * Initialises the scheduler helper with the plugin instance and evaluates the configuration.
     *
     * @param plugin the plugin instance
     */
    public static void init(ProCosmeticsPlugin plugin) {
        Scheduler.plugin = plugin;

        foliaEnabled = FOLIA_AVAILABLE;

        Logger logger = plugin.getLogger();
        if (foliaEnabled) {
            logger.info("Folia scheduler detected; enabling Folia-compatible scheduling.");
        } else {
            logger.info("Folia scheduler API not found; using Bukkit scheduler.");
        }
    }

    private static ProCosmeticsPlugin plugin() {
        if (plugin == null) {
            throw new IllegalStateException("Scheduler has not been initialised yet");
        }
        return plugin;
    }

    /**
     * Runs a task immediately on the appropriate scheduler.
     *
     * @param runnable the task to run
     */
    public static void run(Runnable runnable) {
        if (foliaEnabled) {
            Bukkit.getGlobalRegionScheduler().execute(plugin(), runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin(), runnable);
        }
    }

    /**
     * Runs a task asynchronously.
     *
     * @param runnable the task to run
     */
    public static void runAsync(Runnable runnable) {
        if (foliaEnabled) {
            Bukkit.getAsyncScheduler().runNow(plugin(), task -> runnable.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin(), runnable);
        }
    }

    /**
     * Schedules a task to run after a delay using the global scheduler.
     *
     * @param runnable   the task to run
     * @param delayTicks the delay in ticks before running the task
     * @return the scheduled task handle
     */
    public static Task runLater(Runnable runnable, long delayTicks) {
        if (delayTicks <= 0) {
            run(runnable);
            return new Task(null);
        }

        if (foliaEnabled) {
            return new Task(Bukkit.getGlobalRegionScheduler().runDelayed(plugin(), task -> runnable.run(), delayTicks));
        }
        return new Task(Bukkit.getScheduler().runTaskLater(plugin(), runnable, delayTicks));
    }

    /**
     * Schedules a repeating task using the global scheduler.
     *
     * @param runnable    the task to run
     * @param delayTicks  the initial delay before the first run
     * @param periodTicks the period between runs
     * @return the scheduled task handle
     */
    public static Task runTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (foliaEnabled) {
            return new Task(Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin(), task -> runnable.run(), Math.max(1L, delayTicks), periodTicks));
        }
        return new Task(Bukkit.getScheduler().runTaskTimer(plugin(), runnable, delayTicks, periodTicks));
    }

    /**
     * Runs a task asynchronously after a delay.
     *
     * @param runnable   the task to run
     * @param delayTicks the delay in ticks before running the task
     * @return the scheduled task handle
     */
    public static Task runAsyncLater(Runnable runnable, long delayTicks) {
        if (delayTicks <= 0) {
            runAsync(runnable);
            return new Task(null);
        }

        if (foliaEnabled) {
            return new Task(Bukkit.getAsyncScheduler().runDelayed(plugin(), task -> runnable.run(), delayTicks * 50L, TimeUnit.MILLISECONDS));
        }
        return new Task(Bukkit.getScheduler().runTaskLaterAsynchronously(plugin(), runnable, delayTicks));
    }

    /**
     * Runs a repeating asynchronous task.
     *
     * @param runnable    the task to run
     * @param delayTicks  the initial delay before the first run
     * @param periodTicks the period between runs
     * @return the scheduled task handle
     */
    public static Task runAsyncTimer(Runnable runnable, long delayTicks, long periodTicks) {
        if (foliaEnabled) {
            long initialDelay = Math.max(1L, delayTicks) * 50L;
            long period = Math.max(1L, periodTicks) * 50L;
            return new Task(Bukkit.getAsyncScheduler().runAtFixedRate(plugin(), task -> runnable.run(), initialDelay, period, TimeUnit.MILLISECONDS));
        }
        return new Task(Bukkit.getScheduler().runTaskTimerAsynchronously(plugin(), runnable, delayTicks, periodTicks));
    }

    /**
     * Runs a task immediately using a region-based scheduler.
     *
     * @param location the location context for the task
     * @param runnable the task to run
     */
    public static void run(Location location, Runnable runnable) {
        if (foliaEnabled) {
            Bukkit.getRegionScheduler().execute(plugin(), location, runnable);
        } else {
            Bukkit.getScheduler().runTask(plugin(), runnable);
        }
    }

    /**
     * Runs a task with a delay using a region-based scheduler.
     *
     * @param location   the location context for the task
     * @param runnable   the task to run
     * @param delayTicks the delay in ticks before running the task
     * @return the scheduled task handle
     */
    public static Task runLater(Location location, Runnable runnable, long delayTicks) {
        if (delayTicks <= 0) {
            run(location, runnable);
            return new Task(null);
        }

        if (foliaEnabled) {
            return new Task(Bukkit.getRegionScheduler().runDelayed(plugin(), location, task -> runnable.run(), delayTicks));
        }
        return new Task(Bukkit.getScheduler().runTaskLater(plugin(), runnable, delayTicks));
    }

    /**
     * Runs a repeating task using a region-based scheduler.
     *
     * @param location    the location context for the task
     * @param runnable    the task to run
     * @param delayTicks  the initial delay before the first run
     * @param periodTicks the period between runs
     * @return the scheduled task handle
     */
    public static Task runTimer(Location location, Runnable runnable, long delayTicks, long periodTicks) {
        if (foliaEnabled) {
            return new Task(Bukkit.getRegionScheduler().runAtFixedRate(plugin(), location, task -> runnable.run(), Math.max(1L, delayTicks), periodTicks));
        }
        return new Task(Bukkit.getScheduler().runTaskTimer(plugin(), runnable, delayTicks, periodTicks));
    }

    /**
     * @return {@code true} if Folia scheduling is enabled.
     */
    public static boolean isFolia() {
        return foliaEnabled;
    }

    /**
     * Wrapper around a scheduled task.
     */
    public static class Task {

        private final Object foliaTask;
        private final BukkitTask bukkitTask;

        Task(Object foliaTask) {
            this.foliaTask = foliaTask;
            this.bukkitTask = null;
        }

        Task(BukkitTask bukkitTask) {
            this.foliaTask = null;
            this.bukkitTask = bukkitTask;
        }

        /**
         * Cancels the scheduled task if it is still running.
         */
        public void cancel() {
            if (foliaTask instanceof ScheduledTask scheduledTask) {
                scheduledTask.cancel();
            } else if (bukkitTask != null) {
                bukkitTask.cancel();
            }
        }

        /**
         * @return {@code true} if this task is only a placeholder for an already executed task.
         */
        public boolean isDummy() {
            return foliaTask == null && bukkitTask == null;
        }

        /**
         * @return the underlying Bukkit task when Folia support is disabled, otherwise {@code null}.
         */
        public BukkitTask getBukkitTask() {
            return bukkitTask;
        }
    }
}
