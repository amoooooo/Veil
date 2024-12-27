package foundry.veil.impl;

import foundry.veil.Veil;
import foundry.veil.api.TickTaskScheduler;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@ApiStatus.Internal
public class TickTaskSchedulerImpl implements TickTaskScheduler {

    private final Queue<Task> tasks;
    private long tick;
    private volatile boolean stopped;

    public TickTaskSchedulerImpl() {
        this.tasks = new PriorityBlockingQueue<>();
        this.tick = 0;
        this.stopped = false;
    }

    /**
     * Runs a single tick and executes all pending tasks for that time.
     */
    public void run() {
        Iterator<Task> iterator = this.tasks.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.isDone()) {
                iterator.remove();
                continue;
            }
            if (this.tick < task.executionTick) {
                break;
            }

            try {
                task.runnable.run();
                task.finish(null);
            } catch (Throwable t) {
                Veil.LOGGER.error("Failed to execute task", t);
                task.finish(t);
            }
            iterator.remove();
        }
        this.tick++;
    }

    /**
     * Stops the scheduler and runs all pending tasks as quickly as possible.
     */
    public void shutdown() {
        this.stopped = true;

        Iterator<Task> iterator = this.tasks.iterator();
        while (iterator.hasNext()) {
            Task task = iterator.next();
            if (task.isDone()) {
                iterator.remove();
                continue;
            }

            try {
                task.runnable.run();
                task.finish(null);
            } catch (Throwable t) {
                Veil.LOGGER.error("Failed to execute task", t);
                task.finish(t);
            }
            iterator.remove();
        }
        if (!this.tasks.isEmpty()) {
            throw new IllegalStateException(this.tasks.size() + " tasks were first over!");
        }
    }

    private void validate(Object command) {
        Objects.requireNonNull(command);
        if (this.stopped) {
            throw new RejectedExecutionException();
        }
    }

    @Override
    public void execute(@NotNull Runnable command) {
        this.validate(command);
        this.tasks.add(new Task(command, 0));
    }

    @Override
    public CompletableFuture<?> schedule(@NotNull Runnable command, int delay) {
        this.validate(command);
        if (delay < 0) {
            throw new IllegalArgumentException();
        }

        CompletableFuture<?> future = new CompletableFuture<>();
        Task task = new Task(() -> {
            try {
                command.run();
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }, this.tick + delay);
        this.tasks.add(task);
        future.exceptionally(e -> {
            if (future.isCancelled()) {
                task.cancel(false);
            }
            return null;
        });
        return future;
    }

    @Override
    public <V> CompletableFuture<V> schedule(@NotNull Callable<V> callable, int delay) {
        this.validate(callable);
        if (delay < 0) {
            throw new IllegalArgumentException();
        }

        CompletableFuture<V> future = new CompletableFuture<>();
        Task task = new Task(() -> {
            try {
                future.complete(callable.call());
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }, this.tick + delay);
        this.tasks.add(task);
        future.exceptionally(e -> {
            if (future.isCancelled()) {
                task.cancel(false);
            }
            return null;
        });
        return future;
    }

    @Override
    public CompletableFuture<?> scheduleAtFixedRate(@NotNull Runnable command, int initialDelay, int period) {
        this.validate(command);
        if (initialDelay < 0 || period < 0) {
            throw new IllegalArgumentException();
        }

        CompletableFuture<?> future = new CompletableFuture<>();
        Task task = this.schedule(future, command, new AtomicBoolean(), initialDelay, period);
        this.tasks.add(task);
        future.exceptionally(e -> {
            if (future.isCancelled()) {
                task.cancel(false);
            }
            return null;
        });
        return future;
    }

    private Task schedule(CompletableFuture<?> future, Runnable command, AtomicBoolean cancelled, int delay, int period) {
        return new Task(() -> {
            try {
                command.run();
                if (!this.stopped) {
                    this.tasks.add(this.schedule(future, command, cancelled, period, period));
                }
            } catch (Throwable t) {
                future.completeExceptionally(t);
            }
        }, cancelled, this.tick + delay);
    }

    @Override
    public boolean isShutdown() {
        return this.stopped;
    }

    private class Task implements ScheduledFuture<Object> {

        private final Runnable runnable;
        private final AtomicBoolean cancelled;
        private final long executionTick;
        private boolean complete;
        private Throwable error;

        private Task(Runnable task, long executionTick) {
            this(task, new AtomicBoolean(), executionTick);
        }

        private Task(Runnable task, AtomicBoolean cancelled, long executionTick) {
            this.runnable = task;
            this.cancelled = cancelled;
            this.executionTick = executionTick;
        }

        public void finish(@Nullable Throwable error) {
            this.complete = true;
            this.error = error;
        }

        @Override
        public long getDelay(@NotNull TimeUnit unit) {
            return TimeUnit.MILLISECONDS.convert((this.executionTick - TickTaskSchedulerImpl.this.tick) * 50L, unit);
        }

        @Override
        public int compareTo(@NotNull Delayed o) {
            return Long.compareUnsigned(this.executionTick, ((Task) o).executionTick);
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return this.cancelled.compareAndSet(false, true);
        }

        @Override
        public boolean isCancelled() {
            return this.cancelled.get();
        }

        @Override
        public boolean isDone() {
            return this.complete || this.cancelled.get();
        }

        @Override
        public Object get() throws ExecutionException {
            if (this.error != null) {
                throw new ExecutionException(this.error);
            }
            return null;
        }

        @Override
        public Object get(long timeout, @NotNull TimeUnit unit) throws ExecutionException {
            return this.get();
        }
    }
}
