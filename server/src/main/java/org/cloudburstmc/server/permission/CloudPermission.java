package org.cloudburstmc.server.permission;

import org.cloudburstmc.api.permission.Permissible;
import org.cloudburstmc.api.permission.Permission;
import org.cloudburstmc.server.CloudServer;

import java.util.*;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public class CloudPermission extends Permission {

    public CloudPermission(String name) {
        this(name, null, null, new HashMap<>());
    }

    public CloudPermission(String name, String description) {
        this(name, description, null, new HashMap<>());
    }

    public CloudPermission(String name, String description, String defualtValue) {
        this(name, description, defualtValue, new HashMap<>());
    }

    public CloudPermission(String name, String description, String defualtValue, Map<String, Boolean> children) {
        super(name, description, defualtValue, children);
    }

    public Set<Permissible> getPermissibles() {
        return CloudServer.getInstance().getPermissionManager().getPermissionSubscriptions(this.getName());
    }

    public void recalculatePermissibles() {
        Set<Permissible> perms = this.getPermissibles();

        CloudServer.getInstance().getPermissionManager().recalculatePermissionDefaults(this);

        for (Permissible p : perms) {
            p.recalculatePermissions();
        }
    }

    public void addParent(Permission permission, boolean value) {
        this.getChildren().put(this.getName(), value);
        permission.recalculatePermissibles();
    }

    public Permission addParent(String name, boolean value) {
        Permission perm = CloudServer.getInstance().getPermissionManager().getPermission(name).orElseGet(() -> {
            Permission p = new CloudPermission(name);
            CloudServer.getInstance().getPermissionManager().addPermission(p);

            return p;
        });

        this.addParent(perm, value);

        return perm;
    }

    public static List<CloudPermission> loadPermissions(Map<String, Object> data) {
        return loadPermissions(data, DEFAULT_OP);
    }

    public static List<CloudPermission> loadPermissions(Map<String, Object> data, String defaultValue) {
        List<CloudPermission> result = new ArrayList<>();
        if (data != null) {
            for (Map.Entry e : data.entrySet()) {
                String key = (String) e.getKey();
                Map<String, Object> entry = (Map<String, Object>) e.getValue();
                result.add(loadPermission(key, entry, defaultValue, result));
            }
        }
        return result;
    }

    public static CloudPermission loadPermission(String name, Map<String, Object> data) {
        return loadPermission(name, data, DEFAULT_OP, new ArrayList<>());
    }

    public static CloudPermission loadPermission(String name, Map<String, Object> data, String defaultValue) {
        return loadPermission(name, data, defaultValue, new ArrayList<>());
    }

    public static CloudPermission loadPermission(String name, Map<String, Object> data, String defaultValue, List<CloudPermission> output) {
        String desc = null;
        Map<String, Boolean> children = new HashMap<>();
        if (data.containsKey("default")) {
            String value = CloudPermission.getByName(String.valueOf(data.get("default")));
            if (value != null) {
                defaultValue = value;
            } else {
                throw new IllegalStateException("'default' key contained unknown value");
            }
        }

        if (data.containsKey("children")) {
            if (data.get("children") instanceof Map) {
                for (Map.Entry entry : ((Map<String, Object>) data.get("children")).entrySet()) {
                    String k = (String) entry.getKey();
                    Object v = entry.getValue();
                    if (v instanceof Map) {
                        CloudPermission permission = loadPermission(k, (Map<String, Object>) v, defaultValue, output);
                        if (permission != null) {
                            output.add(permission);
                        }
                    }
                    children.put(k, true);
                }
            } else {
                throw new IllegalStateException("'children' key is of wrong type");
            }
        }

        if (data.containsKey("description")) {
            desc = (String) data.get("description");
        }

        return new CloudPermission(name, desc, defaultValue, children);
    }

}
