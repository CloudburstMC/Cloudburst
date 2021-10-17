package org.cloudburstmc.api.block;

import org.cloudburstmc.api.blockentity.BlockEntityType;
import org.cloudburstmc.api.data.DataKey;
import org.cloudburstmc.api.data.SimpleDataKey;
import org.cloudburstmc.api.entity.Entity;
import org.cloudburstmc.api.item.ItemStack;
import org.cloudburstmc.api.player.Player;
import org.cloudburstmc.api.util.AxisAlignedBB;
import org.cloudburstmc.api.util.Direction;
import org.cloudburstmc.api.util.Identifier;
import org.cloudburstmc.api.util.function.*;

import java.util.Optional;
import java.util.function.*;
import java.util.random.RandomGenerator;

public final class BlockBehaviors {

    private BlockBehaviors() {
    }

    public static final SimpleDataKey<Function<BlockState, String>> GET_DESCRIPTION_ID = DataKey.simple(Identifier.fromString("get_description_id"), Function.class);

    public static final SimpleDataKey<Function<BlockState, AxisAlignedBB>> GET_BOUNDING_BOX = DataKey.simple(Identifier.fromString("get_bounding_box"), Function.class);

    public static final SimpleDataKey<Predicate<BlockState>> CAN_BE_SILK_TOUCHED = DataKey.simple(Identifier.fromString("can_be_silk_touched"), Predicate.class);

    public static final SimpleDataKey<Predicate<BlockState>> CAN_BE_USED_IN_COMMANDS = DataKey.simple(Identifier.fromString("can_be_used_in_commands"), Predicate.class);

    public static final SimpleDataKey<Predicate<BlockState>> CAN_CONTAIN_LIQUID = DataKey.simple(Identifier.fromString("can_contain_liquid"), Predicate.class);

    public static final SimpleDataKey<Predicate<BlockState>> CAN_SPAWN_ON = DataKey.simple(Identifier.fromString("can_spawn_on"), Predicate.class);

    public static final SimpleDataKey<ToFloatFunction<BlockState>> GET_FRICTION = DataKey.simple(Identifier.fromString("get_friction"), ToFloatFunction.class);

    public static final SimpleDataKey<ToFloatFunction<BlockState>> GET_HARDNESS = DataKey.simple(Identifier.fromString("get_hardness"), ToFloatFunction.class);

    public static final SimpleDataKey<ToIntFunction<BlockState>> GET_BURN_ODDS = DataKey.simple(Identifier.fromString("get_burn_odds"), ToIntFunction.class);

    public static final SimpleDataKey<ToIntFunction<BlockState>> GET_FLAME_ODDS = DataKey.simple(Identifier.fromString("get_flame_odds"), ToIntFunction.class);

    public static final SimpleDataKey<ToFloatFunction<BlockState>> GET_GRAVITY = DataKey.simple(Identifier.fromString("get_gravity"), ToFloatFunction.class);

    public static final SimpleDataKey<ToFloatFunction<BlockState>> GET_THICKNESS = DataKey.simple(Identifier.fromString("get_thickness"), ToFloatFunction.class);

    public static final SimpleDataKey<ToFloatFunction<BlockState>> GET_DESTROY_SPEED = DataKey.simple(Identifier.fromString("get_destroy_speed"), ToFloatFunction.class);

    public static final SimpleDataKey<ToIntBiFunction<BlockState, RandomGenerator>> GET_EXPERIENCE_DROP = DataKey.simple(Identifier.fromString("get_experience_drop"), ToIntBiFunction.class);

    public static final SimpleDataKey<ToFloatBiFunction<BlockState, RandomGenerator>> GET_EXPLOSION_RESISTANCE = DataKey.simple(Identifier.fromString("get_explosion_resistance"), ToFloatBiFunction.class);

    public static final SimpleDataKey<Function<Block, ItemStack>> GET_SILK_TOUCH_ITEM_STACK = DataKey.simple(Identifier.fromString("get_silk_touch_item_stack"), Function.class);

    public static final SimpleDataKey<Function<BlockState, BlockState>> GET_STRIPPED_BLOCK = DataKey.simple(Identifier.fromString("get_stripped_block"), Function.class);

    public static final SimpleDataKey<Optional<BlockEntityType<?>>> GET_BLOCK_ENTITY = DataKey.simple(Identifier.fromString("get_block_entity"), Optional.class);

    public static final SimpleDataKey<Predicate<BlockState>> MAY_PICK = DataKey.simple(Identifier.fromString("may_pick"), Predicate.class);

    public static final SimpleDataKey<BiPredicate<Block, Direction>> MAY_PLACE = DataKey.simple(Identifier.fromString("may_place"), BiPredicate.class);

    public static final SimpleDataKey<Predicate<Block>> MAY_PLACE_ON = DataKey.simple(Identifier.fromString("may_place_on"), Predicate.class);

    public static final SimpleDataKey<BiConsumer<Block, Entity>> ON_DESTROY = DataKey.simple(Identifier.fromString("on_destroy"), BiConsumer.class);

    public static final SimpleDataKey<BiConsumer<Block, Block>> ON_NEIGHBOUR_CHANGED = DataKey.simple(Identifier.fromString("on_neighbour_changed"), BiConsumer.class);

    public static final SimpleDataKey<ObjObjFloatConsumer<Block, Entity>> ON_FALL_ON = DataKey.simple(Identifier.fromString("on_fall_on"), ObjObjFloatConsumer.class);

    public static final SimpleDataKey<Consumer<Block>> ON_LIGHTNING_HIT = DataKey.simple(Identifier.fromString("on_lightning_hit"), Consumer.class);

    public static final SimpleDataKey<Consumer<Block>> ON_PLACE = DataKey.simple(Identifier.fromString("on_place"), Consumer.class);

    public static final SimpleDataKey<BiConsumer<Block, Entity>> ON_PROJECTILE_HIT = DataKey.simple(Identifier.fromString("on_projectile_hit"), BiConsumer.class);

    public static final SimpleDataKey<ObjIntConsumer<Block>> ON_REDSTONE_UPDATE = DataKey.simple(Identifier.fromString("on_redstone_update"), ObjIntConsumer.class);

    public static final SimpleDataKey<Consumer<Block>> ON_REMOVE = DataKey.simple(Identifier.fromString("on_remove"), Consumer.class);

    public static final SimpleDataKey<BiConsumer<Block, RandomGenerator>> ON_RANDOM_TICK = DataKey.simple(Identifier.fromString("on_random_tick"), BiConsumer.class);

    public static final SimpleDataKey<TriPredicate<Block, Player, Direction>> USE = DataKey.simple(Identifier.fromString("use"), TriPredicate.class);
}
