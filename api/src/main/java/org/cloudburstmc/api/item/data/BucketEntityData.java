package org.cloudburstmc.api.item.data;

/**
 * Entity state retained by a bucket.
 *
 * @param health captured entity health
 * @param invulnerable whether the captured entity is invulnerable
 * @param immobile whether the captured entity is immobile
 */
public record BucketEntityData(float health, boolean invulnerable, boolean immobile) {
}
