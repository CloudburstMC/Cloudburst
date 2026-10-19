package org.cloudburstmc.server.configuration.validation;

/**
 * A rejected setting, its supplied value, and the requirement it violates.
 */
public record ConfigurationProblem(String path, Object value, String requirement) {
}
