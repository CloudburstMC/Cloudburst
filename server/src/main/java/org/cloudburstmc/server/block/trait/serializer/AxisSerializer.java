package org.cloudburstmc.server.block.trait.serializer;

import com.nukkitx.nbt.NbtMapBuilder;
import org.cloudburstmc.server.block.BlockState;
import org.cloudburstmc.server.block.BlockTypes;
import org.cloudburstmc.server.block.trait.BlockTraitSerializers.TraitSerializer;
import org.cloudburstmc.server.math.Direction.Axis;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class AxisSerializer implements TraitSerializer<Axis> {

    @Override
    public Comparable<?> serialize(NbtMapBuilder builder, BlockState state, Axis axis) {
        if (state.getType() == BlockTypes.PORTAL && axis == Axis.Y) {
            return "unknown";
        }

        return null;
    }
}
