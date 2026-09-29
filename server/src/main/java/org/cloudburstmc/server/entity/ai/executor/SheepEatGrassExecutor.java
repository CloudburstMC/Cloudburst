package org.cloudburstmc.server.entity.ai.executor;

import org.cloudburstmc.api.entity.EntityIntelligent;
import org.cloudburstmc.server.entity.passive.EntitySheep;

/**
 * Extended grass eating executor for sheep that regrows wool after eating.
 *
 * @author daoge_cmd
 */
public class SheepEatGrassExecutor extends EatGrassExecutor {

    @Override
    protected void onEatGrass(EntityIntelligent entity) {
        super.onEatGrass(entity);
        if (entity instanceof EntitySheep sheep && sheep.isSheared()) {
            sheep.setSheared(false);
        }
    }
}
