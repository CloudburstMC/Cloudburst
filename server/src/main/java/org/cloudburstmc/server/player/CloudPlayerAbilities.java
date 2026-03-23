package org.cloudburstmc.server.player;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.cloudburstmc.api.player.Ability;
import org.cloudburstmc.api.player.PlayerAbilities;
import org.cloudburstmc.protocol.bedrock.data.AbilityLayer;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermission;
import org.cloudburstmc.protocol.bedrock.data.command.CommandPermission;
import org.cloudburstmc.protocol.bedrock.packet.UpdateAbilitiesPacket;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class CloudPlayerAbilities implements PlayerAbilities {

    public static final float DEFAULT_WALK_SPEED = 0.1f;
    public static final float DEFAULT_FLY_SPEED = 0.05f;
    public static final float DEFAULT_VERTICAL_FLY_SPEED = 1.0f;

    /**
     * All abilities declared in the base layer, including FLY_SPEED, WALK_SPEED, and
     * VERTICAL_FLY_SPEED. Speed fields must be present in this set; if absent, the game
     * ignores them and falls back to its own defaults, causing incorrect movement.
     */
    private static final Set<org.cloudburstmc.protocol.bedrock.data.Ability> ALL_LAYER_ABILITIES =
            EnumSet.of(
                    org.cloudburstmc.protocol.bedrock.data.Ability.BUILD,
                    org.cloudburstmc.protocol.bedrock.data.Ability.MINE,
                    org.cloudburstmc.protocol.bedrock.data.Ability.DOORS_AND_SWITCHES,
                    org.cloudburstmc.protocol.bedrock.data.Ability.OPEN_CONTAINERS,
                    org.cloudburstmc.protocol.bedrock.data.Ability.ATTACK_PLAYERS,
                    org.cloudburstmc.protocol.bedrock.data.Ability.ATTACK_MOBS,
                    org.cloudburstmc.protocol.bedrock.data.Ability.OPERATOR_COMMANDS,
                    org.cloudburstmc.protocol.bedrock.data.Ability.TELEPORT,
                    org.cloudburstmc.protocol.bedrock.data.Ability.INVULNERABLE,
                    org.cloudburstmc.protocol.bedrock.data.Ability.FLYING,
                    org.cloudburstmc.protocol.bedrock.data.Ability.MAY_FLY,
                    org.cloudburstmc.protocol.bedrock.data.Ability.INSTABUILD,
                    org.cloudburstmc.protocol.bedrock.data.Ability.LIGHTNING,
                    org.cloudburstmc.protocol.bedrock.data.Ability.FLY_SPEED,
                    org.cloudburstmc.protocol.bedrock.data.Ability.WALK_SPEED,
                    org.cloudburstmc.protocol.bedrock.data.Ability.MUTED,
                    org.cloudburstmc.protocol.bedrock.data.Ability.WORLD_BUILDER,
                    org.cloudburstmc.protocol.bedrock.data.Ability.NO_CLIP,
                    org.cloudburstmc.protocol.bedrock.data.Ability.PRIVILEGED_BUILDER,
                    org.cloudburstmc.protocol.bedrock.data.Ability.VERTICAL_FLY_SPEED
            );

    private final Map<Ability, Boolean> values = new EnumMap<>(Ability.class);
    private final CloudPlayer player;

    private float walkSpeed = DEFAULT_WALK_SPEED;
    private float flySpeed = DEFAULT_FLY_SPEED;
    private float verticalFlySpeed = DEFAULT_VERTICAL_FLY_SPEED;

    public CloudPlayerAbilities(CloudPlayer player) {
        this.player = player;
    }

    /**
     * Builds a default survival base layer with no active ability flags and default speeds.
     * Use this when constructing an {@link org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket}
     * for a non-player human entity that has no associated {@link CloudPlayerAbilities} instance.
     */
    public static AbilityLayer defaultBaseLayer() {
        AbilityLayer layer = new AbilityLayer();
        layer.setLayerType(AbilityLayer.Type.BASE);
        layer.getAbilitiesSet().addAll(ALL_LAYER_ABILITIES);
        layer.setWalkSpeed(DEFAULT_WALK_SPEED);
        layer.setFlySpeed(DEFAULT_FLY_SPEED);
        layer.setVerticalFlySpeed(DEFAULT_VERTICAL_FLY_SPEED);
        return layer;
    }

    @Override
    public boolean get(Ability ability) {
        return values.getOrDefault(ability, false);
    }

    @Override
    public CloudPlayerAbilities set(Ability ability, boolean value) {
        values.put(ability, value);
        return this;
    }

    @Override
    public CloudPlayerAbilities setAll(Set<Ability> enabled) {
        values.clear();
        for (Ability ability : enabled) {
            values.put(ability, true);
        }
        return this;
    }

    @Override
    public float getWalkSpeed() {
        return walkSpeed;
    }

    @Override
    public CloudPlayerAbilities setWalkSpeed(float speed) {
        this.walkSpeed = speed;
        return this;
    }

    @Override
    public float getFlySpeed() {
        return flySpeed;
    }

    @Override
    public CloudPlayerAbilities setFlySpeed(float speed) {
        this.flySpeed = speed;
        return this;
    }

    @Override
    public float getVerticalFlySpeed() {
        return verticalFlySpeed;
    }

    @Override
    public CloudPlayerAbilities setVerticalFlySpeed(float speed) {
        this.verticalFlySpeed = speed;
        return this;
    }

    /**
     * Builds the base {@link AbilityLayer} reflecting the current state of this object.
     * This layer is used both in {@link UpdateAbilitiesPacket} and in the
     * {@link org.cloudburstmc.protocol.bedrock.packet.AddPlayerPacket} when spawning the
     * player for other players to see.
     */
    public AbilityLayer buildBaseLayer() {
        AbilityLayer layer = new AbilityLayer();
        layer.setLayerType(AbilityLayer.Type.BASE);

        layer.getAbilitiesSet().addAll(ALL_LAYER_ABILITIES);

        for (Ability ability : Ability.values()) {
            if (Boolean.TRUE.equals(values.get(ability))) {
                org.cloudburstmc.protocol.bedrock.data.Ability protocolAbility = toProtocol(ability);
                if (protocolAbility != null) {
                    layer.getAbilityValues().add(protocolAbility);
                }
            }
        }

        layer.setFlySpeed(flySpeed);
        layer.setVerticalFlySpeed(verticalFlySpeed);
        layer.setWalkSpeed(walkSpeed);
        return layer;
    }

    @Override
    public void update() {
        UpdateAbilitiesPacket packet = new UpdateAbilitiesPacket();
        packet.setUniqueEntityId(player.getUniqueId());
        packet.setPlayerPermission(player.isOp() ? PlayerPermission.OPERATOR : PlayerPermission.MEMBER);
        packet.setCommandPermission(player.isOp() ? CommandPermission.GAME_DIRECTORS : CommandPermission.ANY);
        packet.setAbilityLayers(new ObjectArrayList<>());

        if (player.isSpectator()) {
            packet.getAbilityLayers().add(buildSpectatorLayer());
        }

        packet.getAbilityLayers().add(buildBaseLayer());
        player.sendPacket(packet);
    }

    /**
     * Builds the spectator overlay {@link AbilityLayer} that is prepended before the
     * BASE layer when the player is in spectator mode. This layer forces no-clip, flight,
     * and invulnerability while zeroing all movement speeds.
     */
    private static AbilityLayer buildSpectatorLayer() {
        AbilityLayer layer = new AbilityLayer();
        layer.setLayerType(AbilityLayer.Type.SPECTATOR);

        layer.getAbilitiesSet().addAll(EnumSet.of(
                org.cloudburstmc.protocol.bedrock.data.Ability.BUILD,
                org.cloudburstmc.protocol.bedrock.data.Ability.MINE,
                org.cloudburstmc.protocol.bedrock.data.Ability.DOORS_AND_SWITCHES,
                org.cloudburstmc.protocol.bedrock.data.Ability.OPEN_CONTAINERS,
                org.cloudburstmc.protocol.bedrock.data.Ability.ATTACK_PLAYERS,
                org.cloudburstmc.protocol.bedrock.data.Ability.ATTACK_MOBS,
                org.cloudburstmc.protocol.bedrock.data.Ability.INVULNERABLE,
                org.cloudburstmc.protocol.bedrock.data.Ability.FLYING,
                org.cloudburstmc.protocol.bedrock.data.Ability.MAY_FLY,
                org.cloudburstmc.protocol.bedrock.data.Ability.INSTABUILD,
                org.cloudburstmc.protocol.bedrock.data.Ability.NO_CLIP
        ));

        layer.getAbilityValues().addAll(EnumSet.of(
                org.cloudburstmc.protocol.bedrock.data.Ability.INVULNERABLE,
                org.cloudburstmc.protocol.bedrock.data.Ability.FLYING,
                org.cloudburstmc.protocol.bedrock.data.Ability.NO_CLIP
        ));

        layer.setFlySpeed(0.0f);
        layer.setWalkSpeed(0.0f);
        layer.setVerticalFlySpeed(0.0f);
        return layer;
    }

    /**
     * Maps a single {@link Ability} to its protocol-level counterpart.
     * Float-typed abilities (FLY_SPEED, WALK_SPEED, VERTICAL_FLY_SPEED) are not
     * present in the API enum and are handled as dedicated speed fields instead.
     */
    private static org.cloudburstmc.protocol.bedrock.data.Ability toProtocol(Ability ability) {
        return switch (ability) {
            case BUILD -> org.cloudburstmc.protocol.bedrock.data.Ability.BUILD;
            case MINE -> org.cloudburstmc.protocol.bedrock.data.Ability.MINE;
            case DOORS_AND_SWITCHES -> org.cloudburstmc.protocol.bedrock.data.Ability.DOORS_AND_SWITCHES;
            case OPEN_CONTAINERS -> org.cloudburstmc.protocol.bedrock.data.Ability.OPEN_CONTAINERS;
            case ATTACK_PLAYERS -> org.cloudburstmc.protocol.bedrock.data.Ability.ATTACK_PLAYERS;
            case ATTACK_MOBS -> org.cloudburstmc.protocol.bedrock.data.Ability.ATTACK_MOBS;
            case OPERATOR_COMMANDS -> org.cloudburstmc.protocol.bedrock.data.Ability.OPERATOR_COMMANDS;
            case TELEPORT -> org.cloudburstmc.protocol.bedrock.data.Ability.TELEPORT;
            case INVULNERABLE -> org.cloudburstmc.protocol.bedrock.data.Ability.INVULNERABLE;
            case FLYING -> org.cloudburstmc.protocol.bedrock.data.Ability.FLYING;
            case MAY_FLY -> org.cloudburstmc.protocol.bedrock.data.Ability.MAY_FLY;
            case INSTABUILD -> org.cloudburstmc.protocol.bedrock.data.Ability.INSTABUILD;
            case LIGHTNING -> org.cloudburstmc.protocol.bedrock.data.Ability.LIGHTNING;
            case MUTED -> org.cloudburstmc.protocol.bedrock.data.Ability.MUTED;
            case WORLD_BUILDER -> org.cloudburstmc.protocol.bedrock.data.Ability.WORLD_BUILDER;
            case NO_CLIP -> org.cloudburstmc.protocol.bedrock.data.Ability.NO_CLIP;
            case PRIVILEGED_BUILDER -> org.cloudburstmc.protocol.bedrock.data.Ability.PRIVILEGED_BUILDER;
        };
    }
}
