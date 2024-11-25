package foundry.veil.impl;

import foundry.veil.Veil;

import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class ThreadTaskScheduler {

    private final int threadCount;
    private final Semaphore semaphore;

    private final CompletableFuture<?> completedFuture;
    private final Supplier<Runnable> source;
    private final Deque<Runnable> queue;
    private final AtomicBoolean running;
    private final AtomicBoolean cancelled;
    private final AtomicInteger finished;

    public ThreadTaskScheduler(String name, int threadCount, Supplier<Runnable> source) {
        this.semaphore = new Semaphore(0);

        this.completedFuture = new CompletableFuture<>();
        this.source = source;
        this.queue = new ConcurrentLinkedDeque<>();
        this.running = new AtomicBoolean(true);
        this.cancelled = new AtomicBoolean(false);
        this.finished = new AtomicInteger(0);

        int spawnedThreads = 0;
        for (int i = 0; i < threadCount; i++) {
            Runnable work = source.get();
            if (work == null) {
                this.running.set(false);
                this.semaphore.release(spawnedThreads);
                break;
            }

            this.queue.add(work);
            this.semaphore.release();

            Thread thread = new Thread(this::run, name + "Thread#" + i);
            thread.setPriority(Math.max(0, Thread.NORM_PRIORITY - 2));
            thread.start();
            spawnedThreads++;
        }

        this.threadCount = spawnedThreads;
    }

    private void run() {
        while (true) {
            Runnable task;

            try {
                this.semaphore.acquire();

                // Pull off existing work from the queue, then try to populate it again
                task = this.queue.poll();
                if (task != null) {
                    Runnable next = this.source.get();
                    if (next == null) {
                        if (this.running.compareAndSet(true, false)) {
                            this.semaphore.release(this.threadCount);
                        }
                    } else {
                        this.queue.add(next);
                        this.semaphore.release();
                    }
                }
            } catch (InterruptedException ignored) {
                continue;
            }

            if (task == null) {
                // Work has been completed, so finish
                if (!this.running.get()) {
                    break;
                }
                continue;
            }

            try {
                task.run();
            } catch (Throwable t) {
                Veil.LOGGER.error("Error running task", t);
            }
        }

        if (this.finished.incrementAndGet() >= this.threadCount) {
            this.completedFuture.complete(null);
        }
    }

    public void cancel() {
        if (this.running.compareAndSet(true, false)) {
            this.cancelled.set(true);
            this.semaphore.release(this.threadCount);
        }
    }

    public CompletableFuture<?> getCompletedFuture() {
        return this.completedFuture;
    }

    public boolean isCancelled() {
        return this.cancelled.get();
    }
}