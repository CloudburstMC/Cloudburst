package org.cloudburstmc.server.container;

import lombok.experimental.UtilityClass;
import org.cloudburstmc.api.inventory.view.CreatedOutputView;
import org.cloudburstmc.api.inventory.view.SlotGroupType;
import org.cloudburstmc.api.util.Identifier;

/**
 * <p>These slot group types are used internally by the server to map
 * {@code ContainerSlotType} buckets to slot group implementations.</p>
 */
@UtilityClass
public class ServerSlotGroupTypes {

    /**
     * The virtual slot that the client uses to communicate crafting results.
     *
     * <p>When a player clicks a crafting result, the client sends a {@code TAKE} action
     * from this slot type at slot index 50. It is not a persistent inventory slot
     * and has no meaning outside an open screen. Use {@link org.cloudburstmc.api.inventory.CrafterScreen#getOutputSlot()}
     * or equivalent methods on typed screens instead.</p>
     */
    public static final SlotGroupType<CreatedOutputView> CREATED_OUTPUT =
            SlotGroupType.of(Identifier.parse("cloudburstmc:slot_group_created_output"), CreatedOutputView.class);
}
