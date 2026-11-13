package org.cloudburstmc.server.entity.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Persistent data of a living entity: everything shared. Health is
 * attribute-backed, so it stays in its runtime holder and travels through the
 * living entity's load/save hooks instead.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class LivingData extends EntityData {
}
