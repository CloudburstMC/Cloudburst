package org.cloudburstmc.server.configuration.type;

/**
 * Selects the algorithm that compresses network traffic.
 */
public enum NetworkCompression {
    ZLIB,
    SNAPPY
}
