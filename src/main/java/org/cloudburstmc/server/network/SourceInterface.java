package org.cloudburstmc.server.network;


/**
 * author: MagicDroidX
 * Nukkit Project
 */
public interface SourceInterface {

    void setMotd(String name);

    boolean process();

    void shutdown();

    void emergencyShutdown();
}
