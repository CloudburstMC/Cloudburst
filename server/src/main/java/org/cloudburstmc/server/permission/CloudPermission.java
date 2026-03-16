package org.cloudburstmc.server.permission;

import lombok.extern.log4j.Log4j2;
import org.cloudburstmc.api.permission.Permissible;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.api.permission.PermissionDefault;
import org.cloudburstmc.server.CloudServer;

import java.util.*;

/**
 * Server-side {@link Permission} implementation that delegates the reactive methods
 * ({@link #getPermissibles}, {@link #recalculatePermissibles}, {@link #addParent}) to the
 * server's {@link CloudPermissionManager}.
 */
@Log4j2
public class CloudPermission extends Permission {

    public CloudPermission(String name) {
        super(name);
    }

    public CloudPermission(String name, String description) {
        super(name, description);
    }

    public CloudPermission(String name, PermissionDefault defaultValue) {
        super(name, defaultValue);
    }

    public CloudPermission(String name, Map<String, Boolean> children) {
        super(name, children);
    }

    public CloudPermission(String name, String description, PermissionDefault defaultValue) {
        super(name, description, defaultValue);
    }

    public CloudPermission(String name, PermissionDefault defaultValue, Map<String, Boolean> children) {
        super(name, defaultValue, children);
    }

    public CloudPermission(String name, String description, PermissionDefault defaultValue, Map<String, Boolean> children) {
        super(name, description, defaultValue, children);
    }

    /**
     * Parses a list of {@link CloudPermission}s from a YAML-style map, using
     * {@link Permission#DEFAULT_PERMISSION} as the fallback default.
     *
     * <p>Malformed entries are logged and skipped; they do not abort the rest of the list.</p>
     */
    public static List<CloudPermission> loadPermissions(Map<?, ?> data) {
        return loadPermissions(data, Permission.DEFAULT_PERMISSION);
    }

    /**
     * Parses a list of {@link CloudPermission}s from a YAML-style map using the given fallback default.
     *
     * <p>Malformed entries are logged and skipped; they do not abort the rest of the list.</p>
     */
    public static List<CloudPermission> loadPermissions(Map<?, ?> data, PermissionDefault defaultValue) {
        return loadPermissions(data, "Could not load permission '%s'", defaultValue);
    }

    /**
     * Parses a list of {@link CloudPermission}s from a YAML-style map.
     *
     * <p>The {@code errorTemplate} may contain a single {@code %s} placeholder that is
     * replaced with the name of the failing permission.</p>
     *
     * <p>Malformed entries are logged at ERROR level and skipped.</p>
     */
    public static List<CloudPermission> loadPermissions(Map<?, ?> data, String errorTemplate, PermissionDefault defaultValue) {
        if (errorTemplate == null) {
            throw new IllegalArgumentException("errorTemplate cannot be null");
        }

        List<CloudPermission> result = new ArrayList<>();
        if (data == null) {
            return result;
        }

        for (Map.Entry<?, ?> entry : data.entrySet()) {
            if (entry.getKey() == null) {
                log.error("Skipping permission entry with null key");
                continue;
            }

            try {
                Object raw = entry.getValue();
                if (!(raw instanceof Map<?, ?> value)) {
                    log.error(
                            "{}: expected a map but found {}",
                            String.format(errorTemplate, entry.getKey()),
                            raw == null ? "null" : raw.getClass().getSimpleName()
                    );
                    continue;
                }

                result.add(loadPermission(entry.getKey().toString(), value, defaultValue, result));
            } catch (Exception ex) {
                log.error("{}",
                        String.format(errorTemplate, entry.getKey()),
                        ex
                );
            }
        }

        return result;
    }

    /**
     * Parses a single {@link CloudPermission} from a YAML-style map using {@link Permission#DEFAULT_PERMISSION}.
     */
    public static CloudPermission loadPermission(String name, Map<?, ?> data) {
        return loadPermission(name, data, Permission.DEFAULT_PERMISSION, null);
    }

    /**
     * Parses a single {@link CloudPermission} from a YAML-style map using the given fallback default.
     */
    public static CloudPermission loadPermission(String name, Map<?, ?> data, PermissionDefault defaultValue) {
        return loadPermission(name, data, defaultValue, null);
    }

    /**
     * Parses a single {@link CloudPermission} from a YAML-style data map.
     *
     * <p>The {@code children} node may be either a {@code Map} (map-style) or an
     * {@code Iterable} (list-style). Map values may be {@code Boolean} grant flags or nested
     * {@code Map}s describing a child permission inline. List entries are always granted as
     * {@code true}; null list entries are silently skipped.</p>
     *
     * @param name         permission node name (lowercased before use)
     * @param data         map containing optional keys: {@code default}, {@code children}, {@code description}
     * @param defaultValue fallback default when {@code default} key is absent or null in YAML
     * @param output       list to append recursively discovered child permissions to; may be {@code null}
     * @throws IllegalArgumentException if {@code data} is null, {@code default} is unrecognized,
     *                                  {@code children} is the wrong type, or a child value is neither
     *                                  a {@code Boolean} nor a nested map
     */
    public static CloudPermission loadPermission(String name, Map<?, ?> data, PermissionDefault defaultValue, List<CloudPermission> output) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data map cannot be null for permission '" + name + "'");
        }

        name = name.toLowerCase(Locale.ROOT);

        String desc = null;
        Map<String, Boolean> children = new LinkedHashMap<>();

        if (data.containsKey("default")) {
            Object rawDefault = data.get("default");
            if (rawDefault != null) {
                PermissionDefault parsed = PermissionDefault.getByName(rawDefault.toString());
                if (parsed == null) {
                    throw new IllegalArgumentException("'default' key contained unknown value '" + rawDefault + "' for permission '" + name + "'");
                }
                defaultValue = parsed;
            }
        }

        if (data.containsKey("children")) {
            Object childrenNode = data.get("children");

            if (childrenNode instanceof Map<?, ?> childMap) {
                for (Map.Entry<?, ?> entry : childMap.entrySet()) {
                    if (entry.getKey() == null) {
                        throw new IllegalArgumentException("Child key cannot be null in permission '" + name + "'");
                    }
                    String childName = entry.getKey().toString().toLowerCase(Locale.ROOT);
                    Object childValue = entry.getValue();
                    if (childValue instanceof Map<?, ?>) {
                        try {
                            CloudPermission child = loadPermission(childName, (Map<?, ?>) childValue, defaultValue, output);
                            if (output != null) {
                                output.add(child);
                            }
                            children.put(child.getName(), true);
                        } catch (Exception ex) {
                            throw new IllegalArgumentException(
                                    "Permission node '" + childName + "' in child of '" + name + "' is invalid", ex);
                        }
                    } else if (childValue instanceof Boolean grant) {
                        children.put(childName, grant);
                    } else {
                        throw new IllegalArgumentException(
                                "Child '" + childName + "' of permission '" + name + "' has an invalid value type: expected Boolean or Map, got "
                                        + (childValue == null ? "null" : childValue.getClass().getName()));
                    }
                }
            } else if (childrenNode instanceof Iterable<?> childList) {
                for (Object entry : childList) {
                    if (entry != null) {
                        children.put(entry.toString().toLowerCase(Locale.ROOT), true);
                    }
                }
            } else {
                throw new IllegalArgumentException("'children' key is of wrong type for permission '" + name + "'");
            }
        }

        if (data.containsKey("description")) {
            Object rawDesc = data.get("description");
            if (rawDesc != null) {
                desc = rawDesc.toString();
            }
        }

        return new CloudPermission(name, desc, defaultValue, children);
    }

    @Override
    public Set<Permissible> getPermissibles() {
        return CloudServer.getInstance().getPermissionManager().getPermissionSubscriptions(getName());
    }

    @Override
    public void recalculatePermissibles() {
        Set<Permissible> perms = getPermissibles();
        CloudServer.getInstance().getPermissionManager().recalculatePermissionDefaults(this);
        for (Permissible p : perms) {
            p.recalculatePermissions();
        }
    }

    @Override
    public void addParent(Permission permission, boolean value) {
        permission.getMutableChildren().put(getName(), value);
        permission.recalculatePermissibles();
    }

    @Override
    public Permission addParent(String name, boolean value) {
        String normalised = name.toLowerCase(Locale.ROOT);
        Permission perm = CloudServer.getInstance().getPermissionManager()
                .getPermission(normalised)
                .orElseGet(() -> {
                    Permission p = new CloudPermission(normalised);
                    CloudServer.getInstance().getPermissionManager().addPermission(p);
                    return p;
                });
        addParent(perm, value);
        return perm;
    }
}
