package org.cloudburstmc.server.entity.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Shared persistence type for living entities. Health is saved from the live attribute.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LivingData extends EntityData {
}
