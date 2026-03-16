package org.cloudburstmc.server.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.scheduler.AsyncScheduler;
import org.cloudburstmc.api.scheduler.ScheduledTask;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Off-tick async scheduler backed by virtual threads and a single-threaded timer.
 * <p>
 * Task state machine (6 states, synchronized on the task instance for transitions
 * that touch the timer future field):
 * <p>
 *   ON_TIMER            task is waiting on the timer executor
 *   SCHEDULED_EXECUTOR  task has been submitted to the virtual-thread executor
 *   EXECUTING           task callback is actively running
 *   EXECUTING_CANCELLED repeating task is running but future runs are canceled
 *   FINISHED            one-shot task completed normally
 *   CANCELLED           task will not run (or will not run again)
 */
@Log4j2
@Singleton
public final class CloudAsyncScheduler implements AsyncScheduler {

    private final Executor executor = Executors.newThreadPerTaskExecutor(
            Thread.ofVirtual()
                    .name("Cloudburst Async Scheduler #", 0)
                    .uncaughtExceptionHandler((thread, t) ->
                            log.error("Uncaught exception on {}", thread.getName(), t))
                    .factory()
    );

    private final ScheduledExecutorService timerThread = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "Cloudburst Async Scheduler Timer");
        t.setPriority(Thread.NORM_PRIORITY + 1);
        t.setUncaughtExceptionHandler((thread, ex) ->
                log.error("Uncaught exception on {}", thread.getName(), ex));
        t.setDaemon(true);
        return t;
    });

    private final Set<AsyncScheduledTask> tasks = ConcurrentHashMap.newKeySet();

    @Inject
    public CloudAsyncScheduler() {
    }

    /**
     * Returns the underlying virtual-thread executor so that other components
     * (e.g. chunk managers) can submit work without coupling to this scheduler's task tracking.
     */
    public Executor getExecutor() {
        return this.executor;
    }

    @Override
    public ScheduledTask runNow(PluginContainer plugin, Consumer<ScheduledTask> task) {
        // plugin may be null for internal server tasks
        Objects.requireNonNull(task, "Task may not be null");

        AsyncScheduledTask ret = new AsyncScheduledTask(plugin, -1L, task, null, -1L);
        this.tasks.add(ret);
        this.executor.execute(ret);
        return ret;
    }

    @Override
    public ScheduledTask runDelayed(PluginContainer plugin, Consumer<ScheduledTask> task, long delay, TimeUnit unit) {
        // plugin may be null for internal server tasks
        Objects.requireNonNull(task, "Task may not be null");
        Objects.requireNonNull(unit, "TimeUnit may not be null");

        if (delay < 0L) {
            throw new IllegalArgumentException("Delay may not be < 0");
        }

        return scheduleTimerTask(plugin, task, delay, -1L, unit);
    }

    @Override
    public ScheduledTask runAtFixedRate(PluginContainer plugin, Consumer<ScheduledTask> task, long initialDelay, long period, TimeUnit unit) {
        // plugin may be null for internal server tasks
        Objects.requireNonNull(task, "Task may not be null");
        Objects.requireNonNull(unit, "TimeUnit may not be null");

        if (initialDelay < 0L) {
            throw new IllegalArgumentException("Initial delay may not be < 0");
        }

        if (period <= 0L) {
            throw new IllegalArgumentException("Period may not be <= 0");
        }

        return scheduleTimerTask(plugin, task, initialDelay, period, unit);
    }

    @Override
    public void cancelTasks(PluginContainer plugin) {
        // plugin may be null for internal server tasks; treat null as a valid owner key
        for (AsyncScheduledTask task : this.tasks) {
            if (task.plugin == plugin) {
                task.cancel();
            }
        }
    }

    @Override
    public void cancelAllTasks() {
        for (AsyncScheduledTask task : this.tasks) {
            task.cancel();
        }
        this.tasks.clear();
    }

    private AsyncScheduledTask scheduleTimerTask(PluginContainer plugin, Consumer<ScheduledTask> task, long initialDelay, long period, TimeUnit unit) {
        AsyncScheduledTask ret = new AsyncScheduledTask(
                plugin,
                period <= 0 ? period : unit.toNanos(period),
                task,
                null,
                System.nanoTime() + unit.toNanos(initialDelay)
        );

        // Synchronize on ret while scheduling so that if the timer fires immediately and
        // calls run() before setDelay() returns, the task state is already consistent.
        synchronized (ret) {
            ret.setDelay(this.timerThread.schedule(ret, initialDelay, unit));
            this.tasks.add(ret);
        }

        return ret;
    }

    private final class AsyncScheduledTask implements ScheduledTask, Runnable {

        private static final int STATE_ON_TIMER = 0;
        private static final int STATE_SCHEDULED_EXECUTOR = 1;
        private static final int STATE_EXECUTING = 2;
        private static final int STATE_EXECUTING_CANCELLED = 3;
        private static final int STATE_FINISHED = 4;
        private static final int STATE_CANCELLED = 5;

        private final PluginContainer plugin;
        private final long repeatDelayNs;
        private Consumer<ScheduledTask> run;
        private ScheduledFuture<?> delay;
        private int state;
        private long scheduleTarget;

        private AsyncScheduledTask(PluginContainer plugin, long repeatDelayNs, Consumer<ScheduledTask> run, ScheduledFuture<?> delay, long firstTarget) {
            this.plugin = plugin;
            this.repeatDelayNs = repeatDelayNs;
            this.run = run;
            this.delay = delay;
            this.state = delay == null ? STATE_SCHEDULED_EXECUTOR : STATE_ON_TIMER;
            this.scheduleTarget = firstTarget;
        }

        private void setDelay(ScheduledFuture<?> delay) {
            this.delay = delay;
            this.state = delay == null ? STATE_SCHEDULED_EXECUTOR : STATE_ON_TIMER;
        }

        @Override
        public void run() {
            boolean repeating = this.isRepeatingTask();
            boolean timer;
            synchronized (this) {
                if (this.state == STATE_ON_TIMER) {
                    timer = true;
                    this.delay = null;
                    this.state = STATE_SCHEDULED_EXECUTOR;
                } else if (this.state == STATE_CANCELLED) {
                    return;
                } else if (this.state == STATE_SCHEDULED_EXECUTOR) {
                    timer = false;
                    this.state = STATE_EXECUTING;
                } else {
                    throw new IllegalStateException("Unexpected state on run: " + this.state);
                }
            }

            if (timer) {
                CloudAsyncScheduler.this.executor.execute(this);
                return;
            }

            try {
                this.run.accept(this);
            } catch (Throwable t) {
                String pluginName = this.plugin != null ? this.plugin.getDescription().getName() : "internal";
                log.error("Exception in async task for plugin {}", pluginName, t);
            } finally {
                boolean removeFromTasks = false;
                synchronized (this) {
                    if (!repeating) {
                        removeFromTasks = true;
                        this.state = STATE_FINISHED;
                    } else if (this.state != STATE_EXECUTING_CANCELLED) {
                        this.state = STATE_ON_TIMER;
                        long currTime = System.nanoTime();
                        long nextDelay = Math.max(0L, this.scheduleTarget + this.repeatDelayNs - currTime);
                        this.scheduleTarget = currTime + nextDelay;
                        this.delay = CloudAsyncScheduler.this.timerThread.schedule(this, nextDelay, TimeUnit.NANOSECONDS);
                    } else {
                        removeFromTasks = true;
                    }
                }

                if (removeFromTasks) {
                    this.run = null;
                    CloudAsyncScheduler.this.tasks.remove(this);
                }
            }
        }

        @Override
        public PluginContainer getOwningPlugin() {
            return this.plugin;
        }

        @Override
        public boolean isRepeatingTask() {
            return this.repeatDelayNs > 0L;
        }

        @Override
        public ScheduledTask.CancelledState cancel() {
            ScheduledFuture<?> pendingDelay = null;
            CancelledState result;
            synchronized (this) {
                switch (this.state) {
                    case STATE_ON_TIMER -> {
                        pendingDelay = this.delay;
                        this.delay = null;
                        this.state = STATE_CANCELLED;
                        result = CancelledState.CANCELLED_BY_CALLER;
                    }
                    case STATE_SCHEDULED_EXECUTOR -> {
                        this.state = STATE_CANCELLED;
                        result = CancelledState.CANCELLED_BY_CALLER;
                    }
                    case STATE_EXECUTING -> {
                        if (!this.isRepeatingTask()) {
                            return CancelledState.RUNNING;
                        }
                        this.state = STATE_EXECUTING_CANCELLED;
                        return CancelledState.NEXT_RUNS_CANCELLED;
                    }
                    case STATE_EXECUTING_CANCELLED -> {
                        return CancelledState.NEXT_RUNS_CANCELLED_ALREADY;
                    }
                    case STATE_FINISHED -> {
                        return CancelledState.ALREADY_EXECUTED;
                    }
                    case STATE_CANCELLED -> {
                        return CancelledState.CANCELLED_ALREADY;
                    }
                    default -> throw new IllegalStateException("Unknown state: " + this.state);
                }
            }

            if (pendingDelay != null) {
                pendingDelay.cancel(false);
            }

            this.run = null;
            CloudAsyncScheduler.this.tasks.remove(this);
            return result;
        }

        @Override
        public ScheduledTask.ExecutionState getExecutionState() {
            synchronized (this) {
                return switch (this.state) {
                    case STATE_ON_TIMER, STATE_SCHEDULED_EXECUTOR -> ExecutionState.IDLE;
                    case STATE_EXECUTING -> ExecutionState.RUNNING;
                    case STATE_EXECUTING_CANCELLED -> ExecutionState.CANCELLED_RUNNING;
                    case STATE_FINISHED -> ExecutionState.FINISHED;
                    case STATE_CANCELLED -> ExecutionState.CANCELLED;
                    default -> throw new IllegalStateException("Unknown state: " + this.state);
                };
            }
        }
    }
}
