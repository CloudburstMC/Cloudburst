package org.cloudburstmc.server.entity.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.cloudburstmc.api.block.BlockState;
import org.cloudburstmc.api.block.BlockStates;
import org.cloudburstmc.server.nbt.annotation.NbtShortBlockState;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class CloudFallingBlockData extends CloudEntityData {

    @JsonProperty(value = "BlockState", access = JsonProperty.Access.READ_ONLY)
    @NbtShortBlockState
    private BlockState blockState = BlockStates.SAND;

    @JsonProperty("Time")
    private int time;

    @JsonProperty("DropItem")
    private boolean dropItem = true;

    @JsonProperty("CancelDrop")
    private boolean cancelDrop;

    @JsonProperty("HurtEntities")
    private boolean hurtEntities;

    @JsonProperty("DamagePerBlock")
    private float damagePerBlock;

    @JsonProperty("MaximumDamage")
    private int maximumDamage = 40;

    @JsonProperty("AutoExpire")
    private boolean autoExpire = true;

    @JsonProperty("LandingSound")
    private String landingSound;

    @JsonProperty("BreakSound")
    private String breakSound;
}
