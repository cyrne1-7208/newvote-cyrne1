package net.kunmc.lab.newvote;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/** Keeps Folia-only API references out of the Java 8 / Spigot 1.15.2 binary. */
final class SchedulerLogic {
    private final NewVote plugin;
    private final List<Task> tasks = new ArrayList<>();
    private final ThreadLocal<Player> currentPlayer = new ThreadLocal<>();
    private volatile long generation;
    private Object globalScheduler;
    private Method runDelayed;
    private Method runAtFixedRate;
    private Method cancel;
    private Method getEntityScheduler;
    private Method execute;

    SchedulerLogic(NewVote plugin) {
        this.plugin = plugin;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
        } catch (ClassNotFoundException ex) {
            return;
        }
        try {
            Class<?> global = Class.forName("io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler");
            Class<?> entity = Class.forName("io.papermc.paper.threadedregions.scheduler.EntityScheduler");
            Class<?> task = Class.forName("io.papermc.paper.threadedregions.scheduler.ScheduledTask");
            globalScheduler = invoke(Bukkit.class.getMethod("getGlobalRegionScheduler"), null);
            runDelayed = global.getMethod("runDelayed", Plugin.class, Consumer.class, long.class);
            runAtFixedRate = global.getMethod("runAtFixedRate", Plugin.class, Consumer.class, long.class, long.class);
            cancel = task.getMethod("cancel");
            getEntityScheduler = Entity.class.getMethod("getScheduler");
            execute = entity.getMethod("execute", Plugin.class, Runnable.class, Runnable.class, long.class);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Unable to initialize Folia schedulers", ex);
        }
    }

    void runForPlayer(Player player, Runnable action) {
        // Queue even on the owning region so a new vote cannot overtake an
        // earlier queued Tab update. Nested effects within our callback run inline.
        if (globalScheduler == null || currentPlayer.get() == player) {
            action.run();
        } else {
            final long taskGeneration = generation;
            // A retired (disconnected) player needs no further display updates.
            invoke(execute, invoke(getEntityScheduler, player), plugin, (Runnable) () -> {
                synchronized (plugin) {
                    if (plugin.isEnabled() && taskGeneration == generation) {
                        currentPlayer.set(player);
                        try {
                            action.run();
                        } finally {
                            currentPlayer.remove();
                        }
                    }
                }
            }, null, 1L);
        }
    }

    Task runTaskLater(Consumer<Task> action, long delay) {
        return schedule(action, delay, 0L);
    }

    Task runTaskTimer(Consumer<Task> action, long delay, long period) {
        return schedule(action, delay, period);
    }

    private Task schedule(Consumer<Task> action, long delay, long period) {
        Task task = new Task();
        Runnable runnable = () -> {
            // Region commands and global callbacks share the same vote-state lock.
            synchronized (plugin) {
                if (task.cancelled || !plugin.isEnabled()) {
                    return;
                }
                try {
                    action.accept(task);
                } finally {
                    if (period == 0L || task.cancelled) {
                        tasks.remove(task);
                    }
                }
            }
        };
        if (globalScheduler == null) {
            BukkitTask handle = period == 0L
                    ? Bukkit.getScheduler().runTaskLater(plugin, runnable, delay)
                    : Bukkit.getScheduler().runTaskTimer(plugin, runnable, delay, period);
            task.cancelAction = handle::cancel;
        } else {
            Consumer<Object> callback = ignored -> runnable.run();
            Object handle = period == 0L
                    ? invoke(runDelayed, globalScheduler, plugin, callback, delay)
                    : invoke(runAtFixedRate, globalScheduler, plugin, callback, delay, period);
            task.cancelAction = () -> invoke(cancel, handle);
        }
        tasks.add(task);
        return task;
    }

    void cancelTasks() {
        generation++;
        for (Task task : tasks) {
            task.cancel();
        }
        tasks.clear();
    }

    private static Object invoke(Method method, Object target, Object... args) {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException ex) {
            throw new IllegalStateException("Scheduler operation failed: " + method.getName(), ex.getCause());
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Scheduler operation failed: " + method.getName(), ex);
        }
    }

    // Registration, execution and cancellation all hold the plugin monitor.
    static final class Task {
        private boolean cancelled;
        private Runnable cancelAction;

        void cancel() {
            if (!cancelled) {
                cancelled = true;
                cancelAction.run();
            }
        }
    }
}
