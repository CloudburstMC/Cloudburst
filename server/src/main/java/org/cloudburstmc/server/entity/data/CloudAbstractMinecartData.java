package org.cloudburstmc.server.entity.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shared persistence type for minecarts. Display state is saved separately.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudAbstractMinecartData extends CloudEntityData {
}
