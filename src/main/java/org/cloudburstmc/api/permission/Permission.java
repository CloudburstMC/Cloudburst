package org.cloudburstmc.api.permission;

import java.util.Map;
import java.util.Set;

/**
 * author: MagicDroidX
 * Nukkit Project
 */
public abstract class Permission {

    public static final String DEFAULT_OP = "op";
    public static final String DEFAULT_NOT_OP = "notop";
    public static final String DEFAULT_TRUE = "true";
    public static final String DEFAULT_FALSE = "false";

    public static final String DEFAULT_PERMISSION = DEFAULT_OP;

    private final String name;
    private String description;
    private Map<String, Boolean> children;
    private String defaultValue;

    public Permission(String name, String description, String defaultValue, Map<String, Boolean> children) {
        this.name = name;
        this.description = description != null ? description : "";
        this.defaultValue = defaultValue != null ? defaultValue : DEFAULT_PERMISSION;
        this.children = children;

        this.recalculatePermissibles();
    }

    public static String getByName(String value) {
        switch (value.toLowerCase()) {
            case "op":
            case "isop":
            case "operator":
            case "isoperator":
            case "admin":
            case "isadmin":
                return DEFAULT_OP;

            case "!op":
            case "notop":
            case "!operator":
            case "notoperator":
            case "!admin":
            case "notadmin":
                return DEFAULT_NOT_OP;

            case "true":
                return DEFAULT_TRUE;

            default:
                return DEFAULT_FALSE;
        }
    }

    public String getName() {
        return name;
    }

    public Map<String, Boolean> getChildren() {
        return children;
    }

    public String getDefault() {
        return defaultValue;
    }

    public void setDefault(String value) {
        if (!value.equals(this.defaultValue)) {
            this.defaultValue = value;
            this.recalculatePermissibles();
        }
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public abstract Set<Permissible> getPermissibles();

    public abstract void recalculatePermissibles();

    public abstract void addParent(Permission permission, boolean value);

    public abstract Permission addParent(String name, boolean value);

}
