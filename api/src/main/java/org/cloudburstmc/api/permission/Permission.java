package org.cloudburstmc.api.permission;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Represents a unique permission node that may be attached to a {@link Permissible}.
 *
 * <p>Permissions form a tree through their children map. If a parent permission is granted,
 * all child permissions are also granted (or negated, depending on the child's polarity).</p>
 *
 * <p>Use {@link #addChild(String, boolean)} and {@link #removeChild(String)} to mutate children
 * safely; they call {@link #recalculatePermissibles()} automatically. Direct mutation through
 * {@link #getMutableChildren()} requires a manual {@link #recalculatePermissibles()} call
 * afterward. Note that {@link #getChildren()} returns a live unmodifiable view of the same
 * underlying map, so mutations via {@link #getMutableChildren()} are immediately visible through
 * the view; concurrent iteration over the view while mutating the mutable map will throw
 * {@link java.util.ConcurrentModificationException}.</p>
 */
public abstract class Permission {

    /**
     * The default grant state applied when a permission has not been explicitly registered.
     * Unregistered permissions default to operator-only access.
     */
    public static final PermissionDefault DEFAULT_PERMISSION = PermissionDefault.OP;

    private final String name;
    private final Map<String, Boolean> children;
    private final Map<String, Boolean> childrenView;
    private PermissionDefault defaultValue;
    private String description;

    public Permission(String name) {
        this(name, null, null, null);
    }

    public Permission(String name, String description) {
        this(name, description, null, null);
    }

    public Permission(String name, PermissionDefault defaultValue) {
        this(name, null, defaultValue, null);
    }

    public Permission(String name, Map<String, Boolean> children) {
        this(name, null, null, children);
    }

    public Permission(String name, String description, PermissionDefault defaultValue) {
        this(name, description, defaultValue, null);
    }

    public Permission(String name, String description, Map<String, Boolean> children) {
        this(name, description, null, children);
    }

    public Permission(String name, PermissionDefault defaultValue, Map<String, Boolean> children) {
        this(name, null, defaultValue, children);
    }

    public Permission(String name, String description, PermissionDefault defaultValue, Map<String, Boolean> children) {
        if (name == null) {
            throw new IllegalArgumentException("Permission name cannot be null");
        }
        this.name = name;
        this.description = description != null ? description : "";
        this.defaultValue = defaultValue != null ? defaultValue : DEFAULT_PERMISSION;
        this.children = children != null ? new LinkedHashMap<>(children) : new LinkedHashMap<>();
        this.childrenView = Collections.unmodifiableMap(this.children);
    }

    /**
     * Returns the fully qualified name of this permission.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns an unmodifiable view of the children map.
     *
     * <p>Keys are permission names; {@code true} means the child is inherited normally,
     * {@code false} means the child is inverted when this permission is granted.</p>
     *
     * <p>This is a live view of the underlying mutable map. To add or remove children, use
     * {@link #addChild(String, boolean)} or {@link #removeChild(String)}, which also trigger
     * {@link #recalculatePermissibles()}. If you need to batch-modify children, obtain the
     * mutable map via {@link #getMutableChildren()} and call {@link #recalculatePermissibles()}
     * manually when done. Be aware that mutating the mutable map while iterating this view
     * will throw {@link java.util.ConcurrentModificationException}.</p>
     */
    public Map<String, Boolean> getChildren() {
        return childrenView;
    }

    /**
     * Returns the mutable underlying children map for bulk modifications.
     *
     * <p>You must call {@link #recalculatePermissibles()} after modifying this map.
     * {@link #getChildren()} is a live unmodifiable view of this same map, so any changes
     * are immediately visible through it.</p>
     */
    public Map<String, Boolean> getMutableChildren() {
        return children;
    }

    /**
     * Adds a child permission and immediately triggers recalculation on all subscribed
     * {@link Permissible}s.
     *
     * @param childName the child permission name
     * @param value     {@code true} to inherit normally, {@code false} to invert
     */
    public void addChild(String childName, boolean value) {
        if (childName == null) {
            throw new IllegalArgumentException("Child permission name cannot be null");
        }
        children.put(childName, value);
        recalculatePermissibles();
    }

    /**
     * Removes a child permission and immediately triggers recalculation on all subscribed
     * {@link Permissible}s. Does nothing if the child is not present.
     *
     * @param childName the child permission name
     */
    public void removeChild(String childName) {
        if (childName == null) {
            throw new IllegalArgumentException("Child permission name cannot be null");
        }
        if (children.remove(childName) != null) {
            recalculatePermissibles();
        }
    }

    /**
     * Returns the default grant state of this permission.
     */
    public PermissionDefault getDefault() {
        return defaultValue;
    }

    /**
     * Sets the default grant state of this permission and triggers recalculation on all
     * subscribed {@link Permissible}s.
     *
     * <p>Uses reference equality ({@code !=}) to detect changes, which is correct here because
     * {@link PermissionDefault} is an enum whose constants are singletons.</p>
     */
    public void setDefault(PermissionDefault value) {
        if (value == null) {
            throw new IllegalArgumentException("Default value cannot be null");
        }
        // Reference equality is intentional: PermissionDefault is an enum, so == is correct.
        if (value != this.defaultValue) {
            this.defaultValue = value;
            recalculatePermissibles();
        }
    }

    /**
     * Returns a brief description of this permission.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description of this permission.
     */
    public void setDescription(String description) {
        this.description = description != null ? description : "";
    }

    /**
     * Returns all {@link Permissible}s that are subscribed to this permission.
     */
    public abstract Set<Permissible> getPermissibles();

    /**
     * Triggers recalculation on all {@link Permissible}s subscribed to this permission.
     *
     * <p>Call this after bulk-modifying {@link #getMutableChildren()}.</p>
     */
    public abstract void recalculatePermissibles();

    /**
     * Registers this permission as a child of the given parent permission.
     *
     * @param permission the parent to add this permission to
     * @param value      {@code true} to inherit normally, {@code false} to invert
     */
    public abstract void addParent(Permission permission, boolean value);

    /**
     * Registers this permission as a child of the named parent, creating the parent if needed.
     *
     * @param name  the name of the parent permission
     * @param value {@code true} to inherit normally, {@code false} to invert
     * @return the parent permission
     */
    public abstract Permission addParent(String name, boolean value);
}
