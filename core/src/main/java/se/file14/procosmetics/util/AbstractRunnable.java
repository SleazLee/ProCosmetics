/*
 * This file is part of ProCosmetics - https://github.com/File14/ProCosmetics
 * Copyright (C) 2025 File14 and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package se.file14.procosmetics.util;

import org.bukkit.plugin.Plugin;

import se.file14.procosmetics.util.Scheduler.Task;

public abstract class AbstractRunnable implements Runnable {

    private Task task;

    public Task runTask(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Scheduler.runLater(this, 0L));
    }

    public Task runTaskAsynchronously(Plugin plugin) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Scheduler.runAsyncLater(this, 0L));
    }

    public Task runTaskLater(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Scheduler.runLater(this, delay));
    }

    public Task runTaskLaterAsynchronously(Plugin plugin, long delay) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Scheduler.runAsyncLater(this, delay));
    }

    public Task runTaskTimer(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Scheduler.runTimer(this, delay, period));
    }

    public Task runTaskTimerAsynchronously(Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
        checkNotYetScheduled();
        return setupTask(Scheduler.runAsyncTimer(this, delay, period));
    }

    private Task setupTask(Task task) {
        this.task = task;
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

    private void checkNotYetScheduled() {
        if (task != null)
            throw new IllegalStateException("Already scheduled");
    }
}
