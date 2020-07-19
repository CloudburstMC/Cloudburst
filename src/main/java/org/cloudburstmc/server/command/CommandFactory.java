package org.cloudburstmc.server.command;

@FunctionalInterface
public interface CommandFactory {

    Command create(String name);
}
