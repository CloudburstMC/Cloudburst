package org.cloudburstmc.server.scheduler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.plugin.PluginContainer;
import org.cloudburstmc.api.scheduler.GlobalScheduler;
import org.cloudburstmc.api.scheduler.ScheduledTask;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Runs tick-bound tasks on the server thread. Deadlines use an independent long tick counter.
 */
@Log4j2
@Singleton
public final class CloudGlobalScheduler implements GlobalScheduler {

    private final Object stateLock = new Object();
    private final Thread owner = Thread.currentThread();
    private final Long2ObjectOpenHashMap<List<GlobalScheduledTask>> tasksByDeadline = new Long2ObjectOpenHashMap<>();
    private long tickCount = 0L;

    @Inject
    public CloudGlobalScheduler() {
    }

    /**
     * Advances one simulation tick and runs the tasks due on that tick.
     */
    public void tick() {
        if (Thread.currentThread() != this.owner) {
            throw new IllegalStateException("Global scheduler ticks must run on the server thread");
        }

        final List<GlobalScheduledTask> run;
        synchronized (this.stateLock) {
            this.tickCount = Math.incrementExact(this.tickCount);
            if (this.tasksByDeadline.isEmpty()) {
                run = null;
            } else {
                run = this.tasksByDeadline.remove(this.tickCount);
            }
        }

        if (run == null) {
            return;
        }

        for (GlobalScheduledTask globalScheduledTask : run) {
            globalScheduledTask.run();
        }
    }

    @Override
    public void execute(PluginContainer plugin, Runnable run) {
        Objects.requireNonNull(run, "Runnable may not be null");
        this.run(plugin, task -> run.run());
    }

    @Override
    public ScheduledTask run(PluginContainer plugin, Consumer<ScheduledTask> task) {
        return this.runDelayed(plugin, task, 1L);
    }

    @Override
    public ScheduledTask runDelayed(PluginContainer plugin, Consumer<ScheduledTask> task, long delayTicks) {
        Objects.requireNonNull(task, "Task may not be null");
        if (delayTicks <= 0) {
            throw new IllegalArgumentException("Delay ticks must be > 0");
        }

        GlobalScheduledTask ret = new GlobalScheduledTask(plugin, -1L, task);
        scheduleInternal(ret, delayTicks);
        return ret;
    }

    @Override
    public ScheduledTask runAtFixedRate(PluginContainer plugin, Consumer<ScheduledTask> task, long initialDelayTicks, long periodTicks) {
        Objects.requireNonNull(task, "Task may not be null");

        if (initialDelayTicks <= 0) {
            throw new IllegalArgumentException("Initial delay ticks must be > 0");
        }

        if (periodTicks <= 0) {
            throw new IllegalArgumentException("Period ticks must be > 0");
        }

        GlobalScheduledTask ret = new GlobalScheduledTask(plugin, periodTicks, task);
        scheduleInternal(ret, initialDelayTicks);
        return ret;
    }

    @Override
    public void cancelTasks(PluginContainer plugin) {
        List<GlobalScheduledTask> toCancel = new ArrayList<>();
        synchronized (this.stateLock) {
            for (List<GlobalScheduledTask> bucket : this.tasksByDeadline.values()) {
                for (GlobalScheduledTask t : bucket) {
                    if (t.plugin == plugin) {
                        toCancel.add(t);
                    }
                }
            }
        }

        for (GlobalScheduledTask globalScheduledTask : toCancel) {
            globalScheduledTask.cancel();
        }
    }

    @Override
    public void cancelAllTasks() {
        List<GlobalScheduledTask> toCancel = new ArrayList<>();
        synchronized (this.stateLock) {
            for (List<GlobalScheduledTask> bucket : this.tasksByDeadline.values()) {
                toCancel.addAll(bucket);
            }
            this.tasksByDeadline.clear();
        }

        for (GlobalScheduledTask globalScheduledTask : toCancel) {
            globalScheduledTask.cancel();
        }
    }

    private void scheduleInternal(GlobalScheduledTask task, long delay) {
        synchronized (this.stateLock) {
            this.tasksByDeadline
                    .computeIfAbsent(Math.addExact(this.tickCount, delay), k -> new ArrayList<>())
                    .add(task);
        }
    }

    private final class GlobalScheduledTask implements ScheduledTask, Runnable {

        private static final int STATE_IDLE = 0;
        private static final int STATE_EXECUTING = 1;
        private static final int STATE_EXECUTING_CANCELLED = 2;
        private static final int STATE_FINISHED = 3;
        private static final int STATE_CANCELLED = 4;

        private final PluginContainer plugin;
        private final long repeatDelay;
        private Consumer<ScheduledTask> run;
        private final AtomicInteger state = new AtomicInteger(STATE_IDLE);

        private GlobalScheduledTask(PluginContainer plugin, long repeatDelay, Consumer<ScheduledTask> run) {
            this.plugin = plugin;
            this.repeatDelay = repeatDelay;
            this.run = run;
        }

        @Override
        public void run() {
            boolean repeating = this.isRepeatingTask();
            if (!this.state.compareAndSet(STATE_IDLE, STATE_EXECUTING)) {
                return;
            }

            try {
                this.run.accept(this);
            } catch (Throwable t) {
                String pluginName = this.plugin != null ? this.plugin.getDescription().getName() : "internal";
                log.error("Exception in global task for plugin {}", pluginName, t);
            } finally {
                boolean reschedule = false;
                if (!repeating) {
                    this.state.set(STATE_FINISHED);
                } else if (this.state.compareAndSet(STATE_EXECUTING, STATE_IDLE)) {
                    reschedule = true;
                } else {
                    this.state.set(STATE_CANCELLED);
                }

                if (!reschedule) {
                    this.run = null;
                } else {
                    CloudGlobalScheduler.this.scheduleInternal(this, this.repeatDelay);
                }
            }
        }

        @Override
        public PluginContainer getOwningPlugin() {
            return this.plugin;
        }

        @Override
        public boolean isRepeatingTask() {
            return this.repeatDelay > 0;
        }

        @Override
        public ScheduledTask.CancelledState cancel() {
            for (int curr = this.state.get(); ; ) {
                switch (curr) {
                    case STATE_IDLE -> {
                        int witness = this.state.compareAndExchange(STATE_IDLE, STATE_CANCELLED);
                        if (witness == STATE_IDLE) {
                            this.run = null;
                            return CancelledState.CANCELLED_BY_CALLER;
                        }
                        curr = witness;
                    }
                    case STATE_EXECUTING -> {
                        if (!this.isRepeatingTask()) {
                            return CancelledState.RUNNING;
                        }
                        int witness = this.state.compareAndExchange(STATE_EXECUTING, STATE_EXECUTING_CANCELLED);
                        if (witness == STATE_EXECUTING) {
                            return CancelledState.NEXT_RUNS_CANCELLED;
                        }
                        curr = witness;
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
                    default -> throw new IllegalStateException("Unknown state: " + curr);
                }
            }
        }

        @Override
        public ScheduledTask.ExecutionState getExecutionState() {
            int current = this.state.get();
            return switch (current) {
                case STATE_IDLE -> ExecutionState.IDLE;
                case STATE_EXECUTING -> ExecutionState.RUNNING;
                case STATE_EXECUTING_CANCELLED -> ExecutionState.CANCELLED_RUNNING;
                case STATE_FINISHED -> ExecutionState.FINISHED;
                case STATE_CANCELLED -> ExecutionState.CANCELLED;
                default -> throw new IllegalStateException("Unknown state: " + current);
            };
        }
    }
}
