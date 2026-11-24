package org.cloudburstmc.api.item.data;

/**
 * Entity state retained by a bucket.
 *
 * @param health       captured entity health
 * @param invulnerable whether the captured entity is invulnerable
 * @param immobile     whether the captured entity is immobile
 * @param baby         whether the captured entity is a baby
 */
public record BucketEntityData(float health, boolean invulnerable, boolean immobile, boolean baby) {
}
