package org.cloudburstmc.server.configuration.validation;

import lombok.Getter;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reports invalid settings without publishing a partially validated configuration.
 */
@Getter
public class ConfigurationValidationException extends IllegalArgumentException {

    private final Path file;
    private final List<ConfigurationProblem> problems;

    public ConfigurationValidationException(Path file, List<ConfigurationProblem> problems) {
        super("Invalid configuration " + file + ": " + problems.stream()
                .map(problem -> problem.path() + " = " + problem.value() + " " + problem.requirement())
                .collect(Collectors.joining("; ")));
        this.file = file;
        this.problems = List.copyOf(problems);
    }
}
