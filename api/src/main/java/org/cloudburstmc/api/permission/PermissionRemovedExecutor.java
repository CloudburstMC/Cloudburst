package org.cloudburstmc.api.permission;

/**
 * Callback invoked when a {@link PermissionAttachment} is removed from a {@link Permissible}.
 *
 * <p>This is a functional interface and can be used as a lambda.</p>
 */
@FunctionalInterface
public interface PermissionRemovedExecutor {

    /**
     * Called when the given attachment has been removed from its owning permissible.
     *
     * @param attachment the attachment that was removed
     */
    void attachmentRemoved(PermissionAttachment attachment);
}
