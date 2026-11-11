package org.cloudburstmc.api.item;

/**
 * The pattern and material applied to a piece of armor.
 *
 * @param patternId  the registered trim pattern ID
 * @param materialId the registered trim material ID
 */
public record ArmorTrim(String patternId, String materialId) {

    public ArmorTrim {
        if (patternId.isBlank() || materialId.isBlank()) {
            throw new IllegalArgumentException("Trim IDs must not be blank");
        }
    }
}
