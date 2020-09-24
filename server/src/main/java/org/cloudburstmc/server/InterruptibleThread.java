package org.cloudburstmc.server;

/**
 * Represents a thread that can be interrupted.
 *
 * When a Cloudburst server is shutting down, the server finds all threads
 * that implement this interface and interrupts them one by one.
 */
public interface InterruptibleThread {
}
