package org.cloudburstmc.server.permission;

import com.google.inject.Singleton;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionManager;

import java.util.*;

/**
 * Stores permission definitions and refreshes active subjects when the graph changes.
 */
@Singleton
public class CloudPermissionManager implements PermissionManager {

    private final Map<String, Permission> permissions = new LinkedHashMap<>();
    private final Set<CloudPermissible> subjects = Collections.newSetFromMap(new WeakHashMap<>());

    @Override
    public Optional<Permission> getPermission(String name) {
        synchronized (this.permissions) {
            return Optional.ofNullable(this.permissions.get(Permission.normalizeName(name)));
        }
    }

    @Override
    public void registerAll(Collection<Permission> permissions) {
        Objects.requireNonNull(permissions, "permissions");
        if (permissions.isEmpty()) {
            return;
        }

        synchronized (this.permissions) {
            LinkedHashMap<String, Permission> candidate = new LinkedHashMap<>(this.permissions);
            for (Permission permission : permissions) {
                Permission definition = Objects.requireNonNull(permission, "permission");
                String name = definition.name();
                if (candidate.putIfAbsent(name, definition) != null) {
                    throw new IllegalArgumentException("Permission is already registered: " + name);
                }
            }

            validateGraph(candidate);
            this.permissions.clear();
            this.permissions.putAll(candidate);
        }

        this.refreshSubjects();
    }

    @Override
    public void replace(Permission permission) {
        Permission definition = Objects.requireNonNull(permission, "permission");
        String name = definition.name();
        synchronized (this.permissions) {
            if (!this.permissions.containsKey(name)) {
                throw new IllegalArgumentException("Permission is not registered: " + name);
            }

            if (definition.equals(this.permissions.get(name))) {
                return;
            }

            LinkedHashMap<String, Permission> candidate = new LinkedHashMap<>(this.permissions);
            candidate.put(name, definition);
            validateGraph(candidate);
            this.permissions.put(name, definition);
        }

        this.refreshSubjects();
    }

    @Override
    public Optional<Permission> unregister(String name) {
        String normalizedName = Permission.normalizeName(name);
        Permission removed;
        synchronized (this.permissions) {
            removed = this.permissions.get(normalizedName);
            if (removed == null) {
                return Optional.empty();
            }

            LinkedHashMap<String, Permission> candidate = withoutPermission(this.permissions, normalizedName);
            this.permissions.clear();
            this.permissions.putAll(candidate);
        }

        this.refreshSubjects();
        return Optional.of(removed);
    }

    @Override
    public Collection<Permission> permissions() {
        synchronized (this.permissions) {
            return List.copyOf(this.permissions.values());
        }
    }

    public void attach(CloudPermissible subject) {
        synchronized (this.subjects) {
            this.subjects.add(Objects.requireNonNull(subject, "subject"));
        }
    }

    public void detach(CloudPermissible subject) {
        synchronized (this.subjects) {
            this.subjects.remove(Objects.requireNonNull(subject, "subject"));
        }
    }

    private void refreshSubjects() {
        Set<CloudPermissible> snapshot;
        synchronized (this.subjects) {
            snapshot = Set.copyOf(this.subjects);
        }

        for (CloudPermissible subject : snapshot) {
            subject.refresh();
        }
    }

    private static LinkedHashMap<String, Permission> withoutPermission(
            Map<String, Permission> definitions,
            String removedName
    ) {
        LinkedHashMap<String, Permission> candidate = new LinkedHashMap<>(definitions);
        candidate.remove(removedName);
        for (Map.Entry<String, Permission> entry : candidate.entrySet()) {
            Permission permission = entry.getValue();
            if (!permission.children().containsKey(removedName)) {
                continue;
            }

            LinkedHashMap<String, Boolean> children = new LinkedHashMap<>(permission.children());
            children.remove(removedName);
            entry.setValue(permission.toBuilder().children(children).build());
        }
        return candidate;
    }

    private static void validateGraph(Map<String, Permission> definitions) {
        for (Permission permission : definitions.values()) {
            for (String child : permission.children().keySet()) {
                if (!definitions.containsKey(child)) {
                    throw new IllegalArgumentException("Permission " + permission.name()
                            + " references an unregistered child: " + child);
                }
            }
        }

        Set<String> visited = new LinkedHashSet<>();
        Set<String> path = new LinkedHashSet<>();
        for (String name : definitions.keySet()) {
            visit(name, definitions, visited, path);
        }
    }

    private static void visit(String name, Map<String, Permission> definitions, Set<String> visited,
                              Set<String> path) {
        if (visited.contains(name)) {
            return;
        }

        if (!path.add(name)) {
            throw new IllegalArgumentException("Permission inheritance cycle: "
                    + String.join(" -> ", path) + " -> " + name);
        }

        Permission permission = definitions.get(name);
        if (permission != null) {
            for (String child : permission.children().keySet()) {
                if (definitions.containsKey(child)) {
                    visit(child, definitions, visited, path);
                }
            }
        }

        path.remove(name);
        visited.add(name);
    }
}
