package se.file14.procosmetics.util;

import org.bukkit.plugin.Plugin;

import java.util.function.Supplier;

public abstract class AbstractRunnable implements Runnable {

    private Scheduler.Task task;

    public Scheduler.Task runTask(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
        return scheduleTask(() -> Scheduler.runLater(this, 0L));
    }

    public Scheduler.Task runTaskAsynchronously(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
        return scheduleTask(() -> Scheduler.runAsyncLater(this, 0L));
    }

    public Scheduler.Task runTaskLater(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        return scheduleTask(() -> Scheduler.runLater(this, delay));
    }

    public Scheduler.Task runTaskLaterAsynchronously(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        return scheduleTask(() -> Scheduler.runAsyncLater(this, delay));
    }

    public Scheduler.Task runTaskTimer(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        return scheduleTask(() -> Scheduler.runTimer(this, delay, period));
    }

    public Scheduler.Task runTaskTimerAsynchronously(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        return scheduleTask(() -> Scheduler.runAsyncTimer(this, delay, period));
    }

    protected Scheduler.Task scheduleTask(Supplier<Scheduler.Task> scheduler) {
        checkNotYetScheduled();
        return setupTask(scheduler.get());
    }

    protected Scheduler.Task setupTask(Scheduler.Task task) {
        if (task == null || task.isDummy()) {
            this.task = null;
        } else {
            this.task = task;
        }
        return task;
    }

    public void cancel() {
        if (isRunning()) {
            task.cancel();
            task = null;
        }
    }

    public boolean isRunning() {
        return task != null;
    }

    protected void checkNotYetScheduled() {
        if (task != null)
            throw new IllegalStateException("Already scheduled");
    }
}
