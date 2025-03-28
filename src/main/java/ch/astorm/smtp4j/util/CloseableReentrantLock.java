package ch.astorm.smtp4j.util;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A closeable extension of {@code ReentrantLock} that simplifies usage by providing a mechanism
 * to automatically release the lock using the try-with-resources statement.
 * <p>
 * This class wraps the usage of the ReentrantLock while maintaining the ability to properly
 * unlock the resources. Using {@link #lockCloseable}, it creates an {@link AutoCloseable}
 * that can be used in a safe manner to release the lock.
 */
@SuppressWarnings("UnusedReturnValue")
public class CloseableReentrantLock extends ReentrantLock {

    private final ResourceLock unlocker = CloseableReentrantLock.this::unlock;
    private final Condition condition = newCondition();

    /**
     * Acquires the lock and returns a closeable resource that can be used to automatically
     * release the lock when the resource is closed. This method simplifies the usage of
     * the lock by supporting the try-with-resources statement for proper lock management.
     *
     * @return a {@link ResourceLock} instance that, when closed, releases the lock.
     */
    public ResourceLock lockCloseable() {
        lock();
        return unlocker;
    }

    /**
     * Notifies all threads waiting on the associated condition of this lock.
     * This method signals all threads that are currently waiting on the condition,
     * allowing them to proceed. It is intended to be used in conjunction with
     * condition variables to facilitate thread synchronization around a shared resource.
     * <p>
     * This method should typically be used within a locked context to ensure proper
     * synchronization. The lock associated with this condition must be held by the
     * current thread when this method is called.
     *
     * @throws IllegalMonitorStateException if the current thread is not holding the lock
     *                                      associated with this condition.
     */
    public void notifyCondition() {
        condition.signalAll();
    }

    /**
     * Awaits the condition of this lock for the specified amount of time.
     * The calling thread will wait until it is signaled, interrupted, or the
     * specified waiting time elapses.
     *
     * @param time     the maximum time to wait for the condition
     * @param timeUnit the unit of the {@code time} argument
     * @return {@code true} if the condition was signaled before the timeout, {@code false} if the waiting time elapsed
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    public boolean awaitCondition(long time, TimeUnit timeUnit) throws InterruptedException {
        boolean ret = condition.await(time, timeUnit);
        return ret;
    }
}
