package org.cloudburstmc.server.block.trait;

import com.nukkitx.nbt.NbtMapBuilder;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import lombok.experimental.UtilityClass;
import lombok.val;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.trait.serializer.DirectionSerializer;
import org.cloudburstmc.server.block.trait.serializer.StoneSlabSerializer;
import org.cloudburstmc.server.block.trait.serializer.WoodTypeSerializer;
import org.cloudburstmc.server.math.Direction;
import org.cloudburstmc.server.utils.data.StoneSlabType;
import org.cloudburstmc.server.utils.data.WoodType;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@UtilityClass
@ParametersAreNonnullByDefault
public class BlockTraitSerializers {

    private final Reference2ObjectMap<Class<? extends Comparable<?>>, TraitSerializer<?>> serializers = new Reference2ObjectOpenHashMap<>();

    public void init() {
        register(Direction.class, new DirectionSerializer());
        register(WoodType.class, new WoodTypeSerializer());
        register(StoneSlabType.class, new StoneSlabSerializer());
    }

    public void register(Class<? extends Comparable<?>> clazz, TraitSerializer<?> serializer) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(serializer);
        serializers.put(clazz, serializer);
    }

    @SuppressWarnings("ConstantConditions")
    public void serialize(NbtMapBuilder builder, BlockState state, BlockTrait<?> trait) {
        serialize(builder, state, trait, state.getTrait(trait));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void serialize(NbtMapBuilder builder, BlockState state, BlockTrait<?> trait, Comparable<?> value) {
        TraitSerializer serializer = serializers.get(trait.getValueClass());

        String traitName = null;
        if (serializer != null) {
            val v = serializer.serialize(builder, state, value);
            if (v != null) {
                value = v;
            }

            traitName = serializer.getName(state, trait);
        }

        if (value instanceof Enum<?>) {
            value = ((Enum<?>) value).name().toLowerCase();
        }

        if (traitName == null) {
            traitName = trait.getVanillaName();
        }

        builder.put(traitName, value);
    }

    public interface TraitSerializer<T extends Comparable<T>> {

        default String getName(BlockState state, BlockTrait<?> blockTrait) {
            return blockTrait.getVanillaName();
        }

        default Comparable<?> serialize(NbtMapBuilder builder, BlockState state, T t) {
            return null;
        }
    }
}
