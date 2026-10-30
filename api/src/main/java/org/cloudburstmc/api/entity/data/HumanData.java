package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.player.skin.Skin;

/**
 * Persistent data of a human: everything shared plus the skin. The skin is the
 * runtime skin type; its NBT form is produced by the server's serializer, which
 * reproduces today's {@code Skin} compound with its quirks.
 * <p>
 * The inventory and legacy name tag alias are load-only keys preserved through
 * the raw-tag merge; they are not properties here.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class HumanData extends CreatureData {

    @JsonProperty("Skin")
    private Skin skin;

    public HumanData() {
    }
}
