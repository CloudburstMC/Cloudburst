package org.cloudburstmc.server.network.inventory;

public class NetIds {

    public static boolean isValid(int netId) {
        return netId < 0 && (netId & 1) != 0;
    }
}
