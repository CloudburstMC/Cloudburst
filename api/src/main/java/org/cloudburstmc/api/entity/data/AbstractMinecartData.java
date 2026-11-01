package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent data of an abstract minecart. Display state is metadata-backed, so
 * it travels through the load/save hooks and this class stays an empty marker
 * for the branch.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class AbstractMinecartData extends EntityData {
}
