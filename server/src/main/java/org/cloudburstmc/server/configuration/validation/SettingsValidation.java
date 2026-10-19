package org.cloudburstmc.server.configuration.validation;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.configuration.ConfigurationPath;
import org.cloudburstmc.server.configuration.GlobalConfiguration;
import org.cloudburstmc.server.configuration.LevelConfiguration;
import org.cloudburstmc.server.configuration.LevelDefinition;
import org.cloudburstmc.server.configuration.codec.ConfigurationNumbers;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;

/**
 * Applies field constraints to complete settings before they become active.
 */
@UtilityClass
public class SettingsValidation {

    public static void validate(Path file, Object settings) {
        List<ConfigurationProblem> problems = new ArrayList<>();
        inspect(settings, "", problems);

        if (!problems.isEmpty()) {
            throw new ConfigurationValidationException(file, problems);
        }
    }

    public static ConfigurationValidationException mappingFailure(Path file, RuntimeException failure) {
        Throwable cause = failure;
        while (!(cause instanceof JacksonException) && cause.getCause() != null) {
            cause = cause.getCause();
        }

        String path = "<document>";
        Object value = "<unavailable>";
        String requirement = failure.getMessage();

        if (cause instanceof JacksonException mapping) {
            StringBuilder location = new StringBuilder();
            for (JacksonException.Reference reference : mapping.getPath()) {
                if (reference.getPropertyName() != null) {
                    if (!location.isEmpty()) {
                        location.append('.');
                    }

                    location.append(ConfigurationPath.key(reference.getPropertyName()));
                } else if (reference.getIndex() >= 0) {
                    location.append('[').append(reference.getIndex()).append(']');
                }
            }

            if (!location.isEmpty()) {
                path = location.toString();
            }

            if (mapping instanceof InvalidFormatException invalid) {
                value = invalid.getValue();
            }

            requirement = mapping.getOriginalMessage();
        }

        ConfigurationValidationException exception = new ConfigurationValidationException(file, List.of(new ConfigurationProblem(path, value, requirement)));
        exception.initCause(failure);
        return exception;
    }

    private static void inspect(Object settings, String prefix, List<ConfigurationProblem> problems) {
        for (Field field : settings.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }

            String name = field.getName().replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
            String path = prefix.isEmpty() ? name : prefix + "." + name;
            SettingConstraint constraint = field.getAnnotation(SettingConstraint.class);
            Object value;

            try {
                field.setAccessible(true);
                value = field.get(settings);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Unable to inspect setting " + path, e);
            }

            if (value == null) {
                if (constraint == null || !constraint.optional()) {
                    problems.add(new ConfigurationProblem(path, null, "is required"));
                }

                continue;
            }

            check(path, value, constraint, problems);
            inspectValue(path, value, problems);
        }
    }

    private static void check(String path, Object value, SettingConstraint constraint, List<ConfigurationProblem> problems) {
        if (value instanceof Number number) {
            double actual = number.doubleValue();
            double minimum = constraint == null ? Double.NEGATIVE_INFINITY : constraint.min();
            double maximum = constraint == null ? Double.POSITIVE_INFINITY : constraint.max();
            boolean inclusive = constraint == null || constraint.minInclusive();

            if (!Double.isFinite(actual) || (inclusive ? actual < minimum : actual <= minimum) || actual > maximum) {
                problems.add(new ConfigurationProblem(path, value, numericRequirement(minimum, maximum, inclusive)));
            }

            if (constraint != null && constraint.whole()) {
                try {
                    ConfigurationNumbers.longValue(value, path);
                } catch (IllegalArgumentException e) {
                    problems.add(new ConfigurationProblem(path, value, "must be a whole number in the long range"));
                }
            }
        }

        if (constraint == null) {
            return;
        }

        if (constraint.nonBlank() && value instanceof String text && text.isBlank()) {
            problems.add(new ConfigurationProblem(path, value, "must not be blank"));
        }

        if (constraint.types().length != 0 && Arrays.stream(constraint.types()).noneMatch(type -> type.isInstance(value))) {
            problems.add(new ConfigurationProblem(path, value, "must have one of these types: " + Arrays.toString(constraint.types())));
        }
    }

    private static String numericRequirement(double minimum, double maximum, boolean inclusive) {
        StringBuilder requirement = new StringBuilder("must be finite");
        if (Double.isFinite(minimum)) {
            requirement.append(inclusive ? " and >= " : " and > ").append(minimum);
        }

        if (Double.isFinite(maximum)) {
            requirement.append(" and <= ").append(maximum);
        }

        return requirement.toString();
    }

    private static void inspectValue(String path, Object value, List<ConfigurationProblem> problems) {
        if (value instanceof Map<?, ?> map) {
            map.forEach((key, child) -> {
                String childPath = path + "." + ConfigurationPath.key(String.valueOf(key));

                if (!(key instanceof String name) || name.isBlank() || child == null) {
                    problems.add(new ConfigurationProblem(childPath, child, "requires a nonblank key and a value"));
                } else {
                    inspectValue(childPath, child, problems);
                }
            });
        } else if (value instanceof List<?> list) {
            for (int index = 0; index < list.size(); index++) {
                Object element = list.get(index);
                if (!(element instanceof String)) {
                    problems.add(new ConfigurationProblem(path + "[" + index + "]", element, "must be a string"));
                }
            }
        } else if (value.getClass().getEnclosingClass() == GlobalConfiguration.class
                || value.getClass().getEnclosingClass() == LevelConfiguration.class
                || value.getClass() == LevelDefinition.class) {
            if (!value.getClass().isEnum()) {
                inspect(value, path, problems);
            }
        }
    }
}
