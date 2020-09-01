package org.cloudburstmc.server.plugin.util;

public class CycleException extends GraphException {

    public CycleException() {
        super();
    }

    public CycleException(String msg) {
        super(msg);
    }
}
