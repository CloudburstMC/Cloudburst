package org.cloudburstmc.server.event;

import org.cloudburstmc.api.event.Event;

import java.lang.reflect.Method;
import java.util.List;

public interface EventFireHandler {

    void fire(Event event);

    List<ListenerMethod> getMethods();

    interface ListenerMethod extends Comparable<ListenerMethod> {

        Object getListener();

        Method getMethod();

        void run(Event event) throws Exception;
    }
}

