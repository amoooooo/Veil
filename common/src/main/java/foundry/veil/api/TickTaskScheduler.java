package foundry.veil.api;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * Schedules tasks to be run on future ticks.
 *
 * @author Ocelot
 */
public interface TickTaskScheduler extends Executor {

    /**
     * Executes the specified command on the next particle system tick.
     *
     * @param command The runnable task
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException       if command is null
     */
    @Override
    void execute(@NotNull Runnable command);

    /**
     * Schedules the specified command to run in the specified number of ticks.
     *
     * @param command The runnable task
     * @param delay   The delay in ticks
     * @return A future that completes after the task has been run
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException       if command is null
     * @throws IllegalArgumentException   if delay less than or equal to zero
     */
    CompletableFuture<?> schedule(@NotNull Runnable command, int delay);

    /**
     * Schedules the specified command to run in the specified number of ticks.
     *
     * @param callable The callable task
     * @param delay    The delay in ticks
     * @return A future that completes after the task has been run
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException       if command is null
     * @throws IllegalArgumentException   if delay less than or equal to zero
     */
    <V> CompletableFuture<V> schedule(@NotNull Callable<V> callable, int delay);

    /**
     * Schedules the specified command to run after the specified initial delay in ticks and at each fixed time interval in ticks.
     *
     * @param command      The runnable task
     * @param initialDelay The initial delay in ticks
     * @param period       The period between task executions
     * @return A future that completes if there was an error
     * @throws RejectedExecutionException if the task cannot be scheduled for execution
     * @throws NullPointerException       if command is null
     * @throws IllegalArgumentException   if delay less than or equal to zero
     */
    CompletableFuture<?> scheduleAtFixedRate(@NotNull Runnable command, int initialDelay, int period);

    /**
     * @return Whether the executor has shut down and will reject
     */
    boolean isShutdown();
}
