package ch.astorm.smtp4j.util;

public interface ResourceLock extends AutoCloseable {
    /**
     * Unlocking doesn't throw any checked exception.
     */
    @Override
    void close();
}
