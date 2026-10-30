package org.cloudburstmc.api.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.cloudburstmc.api.block.BlockState;

/**
 * Persistent data of a falling block: everything shared plus its state, timers,
 * drop and damage behavior, and optional landing and break sounds.
 */
@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class FallingBlockData extends EntityData {

    @JsonProperty("BlockState")
    private BlockState blockState;

    @JsonProperty("Time")
    private int time;

    @JsonProperty("DropItem")
    private Boolean dropItem;

    @JsonProperty("CancelDrop")
    private boolean cancelDrop;

    @JsonProperty("HurtEntities")
    private boolean hurtEntities;

    @JsonProperty("DamagePerBlock")
    private float damagePerBlock;

    @JsonProperty("MaximumDamage")
    private Integer maximumDamage;

    @JsonProperty("AutoExpire")
    private Boolean autoExpire;

    @JsonProperty("LandingSound")
    private String landingSound;

    @JsonProperty("BreakSound")
    private String breakSound;

    public FallingBlockData() {
    }
}
