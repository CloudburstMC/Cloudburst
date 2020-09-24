package org.cloudburstmc.server.permission;

import org.cloudburstmc.server.player.Player;

/**
 * Represents an object that may become a server operator, such as a
 * {@link Player}.
 */
public interface ServerOperator {

    /**
     * Checks if this object is a server operator
     *
     * @return true if this is an operator
     */
    boolean isOp();

    /**
     * Sets the operator status of this object
     *
     * @param value true if operator status should be given
     */
    void setOp(boolean value);
}
