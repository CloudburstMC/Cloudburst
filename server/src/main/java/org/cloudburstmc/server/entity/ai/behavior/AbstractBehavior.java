package org.cloudburstmc.server.entity.ai.behavior;

import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.entity.ai.behavior.Behavior;
import org.cloudburstmc.api.entity.ai.behavior.BehaviorState;

/**
 * Base class for behaviors that provides default state management.
 *
 * @author daoge_cmd
 */
public abstract class AbstractBehavior implements Behavior {

    @Getter
    @Setter
    protected BehaviorState behaviorState = BehaviorState.STOP;
}
