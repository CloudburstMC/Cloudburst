package org.cloudburstmc.server.block.trait;

import com.nukkitx.nbt.NbtMapBuilder;
import lombok.experimental.UtilityClass;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.serializer.DirectionHelper;
import org.cloudburstmc.server.math.BlockFace;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@UtilityClass
public class BlockTraitSerializers {

    private final Map<Class<? extends Comparable<?>>, BlockTraitSerializer<?>> serializers = new HashMap<>();

    public void init() {
        register(BlockFace.class, (builder, state, value) ->
                DirectionHelper.serialize(builder, state));
    }

    public void register(@Nonnull Class<? extends Comparable<?>> clazz, @Nonnull BlockTraitSerializer<?> serializer) {
        Objects.requireNonNull(clazz);
        Objects.requireNonNull(serializer);
        serializers.put(clazz, serializer);
    }

    @SuppressWarnings("ConstantConditions")
    public void serialize(@Nonnull NbtMapBuilder builder, @Nonnull BlockState state, @Nonnull BlockTrait<?> trait) {
        serialize(builder, state, trait, state.getTrait(trait));
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void serialize(
            @Nonnull NbtMapBuilder builder,
            @Nonnull BlockState state,
            @Nonnull BlockTrait<?> trait,
            @Nonnull Comparable<?> value
    ) {
        BlockTraitSerializer serializer = serializers.get(trait.getValueClass());

        if (serializer != null) {
            value = serializer.serialize(builder, state, value);
        }

        if (value instanceof Enum<?>) {
            value = ((Enum<?>) value).name().toLowerCase();
        }

        builder.put(trait.getName(), value);
    }
}
